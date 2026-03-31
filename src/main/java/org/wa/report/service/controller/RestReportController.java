package org.wa.report.service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wa.report.service.dto.ReportResponseDto;
import org.wa.report.service.enumeration.PeriodType;
import org.wa.report.service.service.ReportService;

@RestController
@RequestMapping("v1/rest/report")
@RequiredArgsConstructor
public class RestReportController {

    private final ReportService reportService;

    @GetMapping("/excel")
    public ReportResponseDto getExcelReport(
            @RequestParam PeriodType period) {
        return reportService.getLastRequestedExcelReportDto(period);
    }

    @GetMapping("/html")
    public ReportResponseDto getHtmlReport(
            @RequestParam PeriodType period) {
        return reportService.getLastRequestedHtmlReportDto(period);
    }

    @GetMapping("/download/excel")
    public ResponseEntity<Void> downloadExcelReport(
            @RequestParam PeriodType period) {
        String downloadUrl = reportService.getExcelDownloadUrl(period);

        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, downloadUrl)
                .build();
    }
}
