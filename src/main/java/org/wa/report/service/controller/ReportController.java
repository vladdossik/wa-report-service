package org.wa.report.service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.wa.report.service.dto.ReportResponseDto;
import org.wa.report.service.enumeration.PeriodType;
import org.wa.report.service.enumeration.ReportType;
import org.wa.report.service.service.ReportService;
import java.util.Map;

@Controller
@RequestMapping("/v1/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/interactive")
    public String interactiveReportPage(
            @RequestParam(required = false, defaultValue = "WEEK") PeriodType period,
            Model model) {

        model.addAttribute("currentPeriod", period);
        model.addAttribute("periods", Map.of(
                "WEEK", "Неделя",
                "MONTH", "Месяц",
                "YEAR", "Год"
        ));

        return "report-dashboard";
    }

    @GetMapping("/html")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getHtmlReport(
            @RequestParam PeriodType period) {
        String downloadUrl = reportService.getHtmlReportContent(period);

        return ResponseEntity.ok(Map.of(
                "url", downloadUrl,
                "period", period.name(),
                "format", ReportType.HTML.toString()
        ));
    }

    @GetMapping("/excel")
    @ResponseBody
    public ResponseEntity<ReportResponseDto> getExcelReport(
            @RequestParam PeriodType period) {
        ReportResponseDto response = reportService.getExcelReport(period);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/excel")
    public ResponseEntity<Void> downloadExcelReport(
            @RequestParam PeriodType period) {

        String downloadUrl = reportService.getExcelDownloadUrl(period);

        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, downloadUrl)
                .build();
    }

    @GetMapping("/html-report")
    @ResponseBody
    public ResponseEntity<ReportResponseDto> getHtmlReportDto(
            @RequestParam PeriodType period) {

        ReportResponseDto response = reportService.getHtmlReport(period);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/html")
    public ResponseEntity<Void> downloadHtmlReport(
            @RequestParam PeriodType period) {

        String downloadUrl = reportService.getHtmlReportContent(period);

        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, downloadUrl)
                .build();
    }

}
