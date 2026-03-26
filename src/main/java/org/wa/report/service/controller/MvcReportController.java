package org.wa.report.service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.wa.report.service.enumeration.PeriodType;
import org.wa.report.service.enumeration.ReportType;
import org.wa.report.service.service.ReportService;
import java.util.Map;

@Controller
@RequestMapping("/v1/mvc/report")
@RequiredArgsConstructor
public class MvcReportController {

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

    @PostMapping("/html")
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
}
