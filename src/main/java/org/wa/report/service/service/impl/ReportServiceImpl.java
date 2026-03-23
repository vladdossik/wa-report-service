package org.wa.report.service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wa.auth.lib.util.AuthContextHolder;
import org.wa.report.service.client.StorageServiceClient;
import org.wa.report.service.dto.CombinedDashboardDto;
import org.wa.report.service.dto.ReportResponseDto;
import org.wa.report.service.enumeration.Bucket;
import org.wa.report.service.enumeration.PeriodType;
import org.wa.report.service.enumeration.ReportType;
import org.wa.report.service.exception.ReportGenerationException;
import org.wa.report.service.generator.ExcelReportGenerator;
import org.wa.report.service.generator.HtmlReportGenerator;
import org.wa.report.service.model.ReportPeriodParams;
import org.wa.report.service.model.ReportRequest;
import org.wa.report.service.repository.ReportRequestRepository;
import org.wa.report.service.service.ReportService;
import org.wa.report.service.service.S3StorageService;
import org.wa.report.service.util.ReportServiceUtil;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ExcelReportGenerator excelReportGenerator;
    private final HtmlReportGenerator htmlReportGenerator;
    private final StorageServiceClient storageServiceClient;
    private final S3StorageService s3StorageService;
    private final ReportRequestRepository requestRepository;
    private final ReportServiceUtil util;
    private static final String CONTENT_TYPE_HTML_REPORT = "text/html; charset=utf-8";
    private static final String CONTENT_TYPE_EXCEL_REPORT =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Override
    public ReportResponseDto getHtmlReport(PeriodType period) {
        UUID userId = AuthContextHolder.getId();
        ReportPeriodParams params = util.getPeriodParams(period);

        log.info("Запрос HTML отчёта. Пользователь: {}, Период: {}, from: {}, to: {}",
                userId, period, params.getFrom(), params.getNow());

        if (util.isCached(period, ReportType.HTML, params.getFrom(), params.getNow())) {
            String reportKey = util.generateReportKey(userId, ReportType.HTML, period, params.getFrom(), params.getNow());
            Optional<Resource> cachedReport = s3StorageService.getReport(reportKey);

            if (cachedReport.isPresent()) {
                log.info("HTML отчёт найден в кэше S3. Ключ: {}", reportKey);

                saveRequestRecord(userId, period, ReportType.HTML, params.getFrom(), params.getNow(),
                        params.getBucket(), true, reportKey);

                String downloadUrl = s3StorageService.getPresignedUrl(reportKey);

                return ReportResponseDto.builder()
                        .cached(true)
                        .downloadUrl(downloadUrl)
                        .format(ReportType.HTML)
                        .period(period)
                        .build();
            }

            log.info("HTML отчёт не найден в кэше. Генерируем новый. Ключ: {}", reportKey);
        }

        return generateAndSaveHtmlReport(userId, period);
    }

    @Override
    public ReportResponseDto getExcelReport(PeriodType period) {
        UUID userId = AuthContextHolder.getId();
        ReportPeriodParams params = util.getPeriodParams(period);

        log.info("Запрос Excel отчёта. Пользователь: {}, Период: {}, from: {}, to: {}",
                userId, period, params.getFrom(), params.getNow());

        if (util.isCached(period, ReportType.EXCEL, params.getFrom(), params.getNow())) {
            String reportKey = util.generateReportKey(userId, ReportType.EXCEL, period, params.getFrom(), params.getNow());
            Optional<Resource> cachedReport = s3StorageService.getReport(reportKey);

            if (cachedReport.isPresent()) {
                log.info("Excel отчёт найден в кэше S3. Ключ: {}", reportKey);

                saveRequestRecord(userId, period, ReportType.EXCEL, params.getFrom(),
                        params.getNow(), params.getBucket(), true, reportKey);

                String downloadUrl = s3StorageService.getPresignedUrl(reportKey);

                return ReportResponseDto.builder()
                        .cached(true)
                        .downloadUrl(downloadUrl)
                        .format(ReportType.EXCEL)
                        .period(period)
                        .build();
            }

            log.info("Excel отчёт не найден в кэше. Генерируем новый. Ключ: {}", reportKey);
        }

        return generateAndSaveExcelReport(userId, period);
    }

    @Override
    public String getHtmlReportContent(PeriodType period) {
        ReportResponseDto response = getHtmlReport(period);

        if (response == null || response.getDownloadUrl() == null) {
            log.error("Не удалось получить HTML отчёт для периода: {}", period);
            throw new ReportGenerationException("Не удалось получить HTML отчёт");
        }

        return response.getDownloadUrl();
    }

    @Override
    public String getExcelDownloadUrl(PeriodType period) {
        UUID userId = AuthContextHolder.getId();
        ReportPeriodParams params = util.getPeriodParams(period);

        String reportKey = util.generateReportKey(
                userId,
                ReportType.EXCEL,
                period,
                params.getFrom(),
                params.getNow()
        );

        if (!s3StorageService.reportExists(reportKey)) {
            getExcelReport(period);
        }

        return s3StorageService.getPresignedUrl(reportKey);
    }

    @Scheduled(cron = "${schedule.report-deletion.cron}")
    public void cleanupOldReports() {
        log.info("Начало очистки старых отчётов из S3");
        s3StorageService.deleteOldReports(Duration.ofDays(7));
        log.info("Очистка старых отчётов из S3 завершена");
    }

    private ReportResponseDto generateAndSaveHtmlReport(UUID userId, PeriodType period) {
        ReportPeriodParams params = util.getPeriodParams(period);

        try {
            CombinedDashboardDto data = storageServiceClient.getData(
                    userId, params.getFrom(), params.getNow(), params.getBucket().getValue()).block();

            if (data == null) {
                throw new ReportGenerationException("Не удалось получить данные для отчёта");
            }

            String excelDownloadUrl = htmlReportGenerator.generateExcelDownloadUrl(
                    period, params.getFrom(), params.getNow(), params.getBucket().getValue());

            Resource report = htmlReportGenerator.generate(data, excelDownloadUrl);

            byte[] content = report.getContentAsByteArray();
            String reportKey = util.generateReportKey(
                    userId, ReportType.HTML, period, params.getFrom(), params.getNow());

            s3StorageService.saveReport(userId, ReportType.HTML, period, params.getFrom(), params.getNow(), content,
                    CONTENT_TYPE_HTML_REPORT);

            saveRequestRecord(userId, period, ReportType.HTML, params.getFrom(),
                    params.getNow(), params.getBucket(), false, reportKey);

            log.info("HTML отчёт успешно сгенерирован. Пользователь: {}, Период: {}", userId, period);

            return ReportResponseDto.builder()
                    .cached(false)
                    .downloadUrl(s3StorageService.getPresignedUrl(reportKey))
                    .format(ReportType.HTML)
                    .period(period)
                    .build();

        } catch (IOException e) {
            log.error("Ошибка при генерации HTML отчёта", e);
            throw new ReportGenerationException("Ошибка при генерации HTML отчёта", e);
        }
    }

    private ReportResponseDto generateAndSaveExcelReport(UUID userId, PeriodType period) {
        ReportPeriodParams params = util.getPeriodParams(period);

        try {
            CombinedDashboardDto data = storageServiceClient.getData(
                    userId, params.getFrom(), params.getNow(), params.getBucket().getValue()).block();

            if (data == null) {
                throw new ReportGenerationException("Не удалось получить данные для отчёта");
            }

            Resource report = excelReportGenerator.generate(data);

            byte[] content = report.getContentAsByteArray();
            String reportKey = util.generateReportKey(
                    userId, ReportType.EXCEL, period, params.getFrom(), params.getNow());

            s3StorageService.saveReport(userId, ReportType.EXCEL, period, params.getFrom(), params.getNow(), content,
                    CONTENT_TYPE_EXCEL_REPORT);

            saveRequestRecord(userId, period, ReportType.EXCEL, params.getFrom(),
                    params.getNow(), params.getBucket(), false, reportKey);

            log.info("Excel отчёт успешно сгенерирован. Пользователь: {}, Период: {}", userId, period);

            return ReportResponseDto.builder()
                    .cached(false)
                    .downloadUrl(s3StorageService.getPresignedUrl(reportKey))
                    .format(ReportType.EXCEL)
                    .period(period)
                    .build();

        } catch (IOException e) {
            log.error("Ошибка при генерации Excel отчёта", e);
            throw new ReportGenerationException("Ошибка при генерации Excel отчёта", e);
        }
    }

    private void saveRequestRecord(UUID userId, PeriodType period, ReportType format,
                                   OffsetDateTime from, OffsetDateTime to, Bucket bucket,
                                   boolean fromCache, String reportKey) {
        try {
            ReportRequest request = ReportRequest.builder()
                    .externalId(userId)
                    .period(period)
                    .format(format)
                    .fromDate(from)
                    .toDate(to)
                    .bucket(bucket)
                    .requestedAt(OffsetDateTime.now())
                    .fromCache(fromCache)
                    .reportKey(reportKey)
                    .build();

            requestRepository.save(request);

            if (fromCache) {
                log.debug("Запрос сохранён (кэш). Пользователь: {}, Период: {}", userId, period);
            } else {
                log.debug("Запрос сохранён (генерация). Пользователь: {}, Период: {}", userId, period);
            }

        } catch (Exception e) {
            log.error("Ошибка сохранения записи запроса", e);
        }
    }

}
