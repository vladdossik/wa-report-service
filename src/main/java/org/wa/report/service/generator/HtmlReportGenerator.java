package org.wa.report.service.generator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.wa.report.service.dto.CombinedDashboardDto;
import org.wa.report.service.exception.ReportGenerationException;
import org.wa.report.service.util.HtmlUtilEngine;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class HtmlReportGenerator {

    private final HtmlUtilEngine htmlUtilEngine;

    private static final DateTimeFormatter FILENAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final String HTML_FILE_PREFIX = "html_report_";
    private static final String HTML_FILE_EXTENSION = ".html";

    public Resource generate(CombinedDashboardDto dto) {
        if (dto == null) {
            throw new ReportGenerationException("Данные для генерации отчёта отсутствуют");
        }

        if ((dto.getMetrics() == null || dto.getMetrics().isEmpty()) &&
                (dto.getActivities() == null || dto.getActivities().isEmpty())) {
            throw new ReportGenerationException("Нет данных для генерации отчёта");
        }

        try {
            log.debug("Старт генерации html-отчёта");

            validateInputData(dto);

            String html = htmlUtilEngine.render(dto);

            if (html == null || html.isEmpty()) {
                throw new ReportGenerationException("Ошибка генерации html-отчёта");
            }

            byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);
            InputStream result = new ByteArrayInputStream(htmlBytes);

            log.info("HTML отчёт сгенерирован. Размер: {} байт", htmlBytes.length);

            return new InputStreamResource(result) {
                @Override
                public String getFilename() {
                    return generateFilename();
                }

                @Override
                public long contentLength() {
                    return htmlBytes.length;
                }
            };

        } catch (ReportGenerationException e) {
            log.error("Ошибка генерации html-отчёта: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при генерации HTML отчета", e);
            throw new ReportGenerationException("Неожиданная ошибка при генерации HTML отчета: " + e.getMessage(), e);
        }
    }

    private void validateInputData(CombinedDashboardDto dto) {
        if (dto == null) {
            throw new ReportGenerationException("Входные данные для отчета не могут быть пустыми");
        }

        if (dto.getMetrics() == null && dto.getActivities() == null) {
            log.warn("Отсутствие данных метрик и активностей");
        }
    }

    private String generateFilename() {
        String timestamp = OffsetDateTime.now().format(FILENAME_FORMATTER);
        return HTML_FILE_PREFIX + timestamp + HTML_FILE_EXTENSION;
    }
}
