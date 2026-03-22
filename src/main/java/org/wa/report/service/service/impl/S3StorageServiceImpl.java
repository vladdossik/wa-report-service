package org.wa.report.service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.wa.report.service.enumeration.PeriodType;
import org.wa.report.service.enumeration.ReportType;
import org.wa.report.service.exception.S3StorageException;
import org.wa.report.service.service.S3StorageService;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements S3StorageService {

    private final S3Client s3Client;

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.report-prefix}")
    private String reportPrefix;

    @Value("${aws.s3.presigned-url-expiration}")
    private long presignedUrlExpiration;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public String saveReport(UUID userId, ReportType format, PeriodType period,
                             OffsetDateTime from, OffsetDateTime to,
                             byte[] content, String contentType) {
        try {
            String key = generateKey(userId, format, period, from, to);

            log.info("Сохранение отчёта в S3. Пользователь: {}, Ключ: {}, Размер: {} байт",
                    userId, key, content.length);

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

            s3Client.putObject(request, RequestBody.fromBytes(content));

            log.info("Отчёт успешно сохранен в S3: {}", key);

            return key;

        } catch (S3Exception e) {
            log.error("Ошибка сохранения отчёта в S3. Пользователь: {}, Формат: {}", userId, format, e);
            throw new S3StorageException("Ошибка сохранения отчёта в S3", e);
        }
    }

    @Override
    public Optional<Resource> getReport(String key) {
        try {
            log.debug("Получение отчёта из S3: {}", key);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
                byte[] content = response.readAllBytes();
                return Optional.of(new ByteArrayResource(content));
            }

        } catch (NoSuchKeyException e) {
            log.debug("Отчёт не найден в S3: {}", key);
            return Optional.empty();
        } catch (IOException | S3Exception e) {
            log.error("Ошибка получения отчёта из S3: {}", key, e);
            throw new S3StorageException("Ошибка получения отчёта из S3", e);
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

            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(reportPrefix + "/")
                    .build();

            ListObjectsV2Response response;
            do {
                response = s3Client.listObjectsV2(request);

                for (S3Object object : response.contents()) {
                    if (object.lastModified().isBefore(cutOffTime.toInstant())) {
                        deleteReport(object.key());
                    }
                }

                request = request.toBuilder()
                        .continuationToken(response.nextContinuationToken())
                        .build();

            } while (response.isTruncated());

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

            log.debug("Сгенерирована временная ссылка для: {}, истекающая через: {}s",
                    key, presignedUrlExpiration);

            return url;

        } catch (S3Exception e) {
            log.error("Ошибка генерации временной ссылки: {}", key, e);
            throw new S3StorageException("Ошибка генерации ссылки на отчёт", e);
        }
    }

    private String generateKey(UUID userId, ReportType format, PeriodType period,
                               OffsetDateTime from, OffsetDateTime to) {
        String datePath = OffsetDateTime.now().format(DATE_FORMATTER);
        String fromDate = from.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String toDate = to.format(DateTimeFormatter.ISO_LOCAL_DATE);

        return String.format("%s/%s/%s/%s/%s_%s_%s.%s",
                reportPrefix,
                userId,
                format.toString().toLowerCase(),
                datePath,
                period.toString().toLowerCase(),
                fromDate,
                toDate,
                format.equals(ReportType.EXCEL) ? "xlsx" : "html");
    }
}
