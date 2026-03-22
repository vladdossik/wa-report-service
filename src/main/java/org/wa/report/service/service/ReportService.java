package org.wa.report.service.service;

import org.wa.report.service.dto.ReportResponseDto;
import org.wa.report.service.enumeration.PeriodType;

public interface ReportService {
    ReportResponseDto getHtmlReport(PeriodType period);
    ReportResponseDto getExcelReport(PeriodType period);
    String getHtmlReportContent(PeriodType period);
    String getExcelDownloadUrl(PeriodType period);
}
