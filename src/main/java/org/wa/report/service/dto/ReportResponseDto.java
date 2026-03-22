package org.wa.report.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.wa.report.service.enumeration.PeriodType;
import org.wa.report.service.enumeration.ReportType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponseDto {
    private String downloadUrl;
    private ReportType format;
    private PeriodType period;
    private boolean cached;
    private Long fileSize;
    private String message;
}
