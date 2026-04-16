package org.wa.report.service.generator;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.wa.report.service.dto.AggregatedActivityDto;
import org.wa.report.service.dto.AggregatedMetricDto;
import org.wa.report.service.dto.CombinedDashboardDto;
import org.wa.report.service.exception.ReportGenerationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class ExcelReportGenerator {

    private static final String[] METRICS_COLUMNS =
            {"Период", "Ср. пульс", "Макс. пульс", "Ср. шаги", "Макс. шаги", "Ср. сон (часы)"};

    private static final int[] METRICS_COLUMN_WIDTHS = {20, 12, 12, 12, 12, 15};

    private static final String[] ACTIVITIES_COLUMNS =
            {"Период", "Активность", "Параметр", "Счётчик активности",
                    "Общее кол-во", "Ср. кол-во", "Мин. кол-во", "Макс. кол-во"};

    private static final int[] ACTIVITIES_COLUMN_WIDTHS = {20, 15, 10, 15, 15, 12, 10, 10};

    private static final DateTimeFormatter FILENAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final String SHEET_METRICS_NAME = "Метрики";
    private static final String SHEET_ACTIVITIES_NAME = "Активности";
    private static final String TITLE_METRICS = "Детальные метрики";
    private static final String TITLE_ACTIVITIES = "Активности";
    private static final String EXCEL_FILE_PREFIX = "excel_report_";
    private static final String EXCEL_FILE_EXTENSION = ".xlsx";

    public Resource generate(CombinedDashboardDto dto) {
        if (dto == null) {
            throw new ReportGenerationException("Данные для генерации отчёта отсутствуют");
        }

        if ((dto.getMetrics() == null || dto.getMetrics().isEmpty()) &&
                (dto.getActivities() == null || dto.getActivities().isEmpty())) {
            throw new ReportGenerationException("Нет данных для генерации отчёта");
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             SXSSFWorkbook workbook = new SXSSFWorkbook(300)) {

                createMetricsSheet(workbook, dto);
                createActivitiesSheet(workbook, dto);

                workbook.write(outputStream);

            byte[] excelBytes = outputStream.toByteArray();
            InputStream result = new ByteArrayInputStream(excelBytes);

            log.info("Excel отчёт сгенерирован. Размер: {} байт", excelBytes.length);

            return new InputStreamResource(result) {
                @Override
                public String getFilename() {
                    return generateFilename();
                }

                @Override
                public long contentLength() {
                    return excelBytes.length;
                }
            };

        } catch (IOException e) {
            log.error("Ошибка при генерации Excel отчета", e);
            throw new ReportGenerationException("Ошибка генерации Excel отчета: " + e.getMessage(), e);
        }
    }

    private void createMetricsSheet(SXSSFWorkbook workbook, CombinedDashboardDto data) {
        SXSSFSheet sheet = workbook.createSheet(SHEET_METRICS_NAME);

        setColumnWidths(sheet, METRICS_COLUMN_WIDTHS);

        List<AggregatedMetricDto> metrics = data.getMetrics();

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(TITLE_METRICS);

        Row headerRow = sheet.createRow(2);
        for (int i = 0; i < METRICS_COLUMNS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(METRICS_COLUMNS[i]);
        }

        int rowNum = 3;
        for (AggregatedMetricDto metric : metrics) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(metric.getBucket().format(DATE_TIME_FORMATTER));
            row.createCell(1).setCellValue(metric.getAvgHeartRate());
            row.createCell(2).setCellValue(metric.getMaxHeartRate());
            row.createCell(3).setCellValue(metric.getAvgSteps());
            row.createCell(4).setCellValue(metric.getMaxSteps());
            row.createCell(5).setCellValue(metric.getAvgSleep());
        }
    }

    private void createActivitiesSheet(SXSSFWorkbook workbook, CombinedDashboardDto data) {
        SXSSFSheet sheet = workbook.createSheet(SHEET_ACTIVITIES_NAME);

        setColumnWidths(sheet, ACTIVITIES_COLUMN_WIDTHS);

        List<AggregatedActivityDto> activities = data.getActivities();

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(TITLE_ACTIVITIES);

        Row headerRow = sheet.createRow(2);
        for (int i = 0; i < ACTIVITIES_COLUMNS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(ACTIVITIES_COLUMNS[i]);
        }

        int rowNum = 3;
        for (AggregatedActivityDto activity : activities) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(activity.getBucket().format(DATE_TIME_FORMATTER));
            row.createCell(1).setCellValue(activity.getEventType());
            row.createCell(2).setCellValue(activity.getUnit());
            row.createCell(3).setCellValue(activity.getEventCount());
            row.createCell(4).setCellValue(activity.getTotalQuantity());
            row.createCell(5).setCellValue(activity.getAvgQuantity());
            row.createCell(6).setCellValue(activity.getMinQuantity());
            row.createCell(7).setCellValue(activity.getMaxQuantity());
        }
    }

    private String generateFilename() {
        String timestamp = OffsetDateTime.now().format(FILENAME_FORMATTER);
        return EXCEL_FILE_PREFIX + timestamp + EXCEL_FILE_EXTENSION;
    }

    private void setColumnWidths(SXSSFSheet sheet, int[] widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }
}
