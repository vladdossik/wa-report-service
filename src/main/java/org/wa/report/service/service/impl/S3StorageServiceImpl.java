package org.wa.report.service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wa.report.service.enumeration.PeriodType;
import org.wa.report.service.enumeration.ReportType;
import org.wa.report.service.exception.S3StorageException;
import org.wa.report.service.service.S3StorageService;
import org.wa.report.service.util.ReportServiceUtil;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements S3StorageService {

    private final S3Client s3Client;

    private final S3Presigner s3Presigner;

    private final ReportServiceUtil util;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiration}")
    private long presignedUrlExpiration;

    @Override
    public String saveReport(UUID userId, ReportType format, PeriodType period,
                             OffsetDateTime from, OffsetDateTime to,
                             InputStream contentStream, String contentType, long contentLength) {
        if (contentStream == null) {
            throw new S3StorageException("InputStream не может быть null");
        }

        try {
            String key = util.generateReportKey(userId, format, period, from, to);

            log.info("Сохранение отчёта в S3. Пользователь: {}, Ключ: {}, Размер: {} байт",
                    userId, key, contentLength);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .metadata(Map.of(
                            "userId", userId.toString(),
                            "format", format.toString(),
                            "period", period.toString(),
                            "from", from.toString(),
                            "to", to.toString(),
                            "generatedAt", OffsetDateTime.now().toString()
                    ))
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(contentStream, contentLength));

            log.info("Отчёт успешно сохранен в S3: {}", key);

            return key;

        } catch (S3Exception e) {
            log.error("Ошибка сохранения отчёта в S3. Пользователь: {}, Формат: {}", userId, format, e);
            throw new S3StorageException("Ошибка сохранения отчёта в S3: " + e.getMessage());
        }
    }

    @Override
    public boolean reportExists(String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(request);
            return true;

        } catch (NoSuchKeyException e) {
            log.error("Такого ключа не существует: {}", key);
            return false;
        } catch (S3Exception e) {
            log.error("Ошибка проверки существования отчёта: {}", key, e);
            return false;
        }
    }

    @Override
    public void deleteOldReports(Duration olderThan) {
        try {
            OffsetDateTime cutOffTime = OffsetDateTime.now().minus(olderThan);
            String prefix = "report/";
            int batchSize = 100;
            int deletedCount = 0;

            log.info("Начало удаления отчётов старше: {}", cutOffTime);

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .maxKeys(batchSize)
                    .build();

            ListObjectsV2Response listResponse;
            boolean hasMore = true;

            while (hasMore) {
                listResponse = s3Client.listObjectsV2(listRequest);

                if (listResponse.contents().isEmpty()) {
                    break;
                }

                List<String> keysToDelete = listResponse.contents().stream()
                        .filter(object -> object.lastModified().isBefore(cutOffTime.toInstant()))
                        .map(S3Object::key)
                        .toList();

                if (!keysToDelete.isEmpty()) {
                    for (String key : keysToDelete) {
                        deleteReport(key);
                        deletedCount++;
                    }
                    log.info("Удалено {} отчётов из {} в текущей итерации",
                            keysToDelete.size(), listResponse.contents().size());
                }

                hasMore = listResponse.isTruncated();
                if (hasMore) {
                    listRequest = listRequest.toBuilder()
                            .continuationToken(listResponse.nextContinuationToken())
                            .build();
                }
            }

            log.info("Очистка завершена. Всего удалено отчётов: {}", deletedCount);

        } catch (S3Exception e) {
            log.error("Ошибка удаления устаревших отчётов", e);
        }
    }

    @Override
    public void deleteReport(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("Отчёт удалён: {}", key);

        } catch (S3Exception e) {
            log.error("Ошибка удаления отчёта: {}", key, e);
        }
    }

    @Override
    public String getPresignedUrl(String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .getObjectRequest(getObjectRequest)
                    .signatureDuration(Duration.ofSeconds(presignedUrlExpiration))
                    .build();

            String url = s3Presigner.presignGetObject(presignRequest).url().toString();

            log.debug("Сгенерирована временная ссылка {} для: {}, истекающая через: {}s", url,
                    key, presignedUrlExpiration);

            return url;

        } catch (S3Exception e) {
            log.error("Ошибка генерации временной ссылки: {}", key, e);
            throw new S3StorageException("Ошибка генерации ссылки на отчёт: " + e);
        }
    }
}
