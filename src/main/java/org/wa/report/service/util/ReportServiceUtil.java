package org.wa.report.service.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.wa.auth.lib.util.AuthContextHolder;
import org.wa.report.service.enumeration.Bucket;
import org.wa.report.service.enumeration.PeriodType;
import org.wa.report.service.enumeration.ReportType;
import org.wa.report.service.exception.RequestNotFoundException;
import org.wa.report.service.model.ReportPeriodParams;
import org.wa.report.service.repository.ReportRequestRepository;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReportServiceUtil {

    private final ReportRequestRepository requestRepository;

    private static final DateTimeFormatter KEY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String generateReportKey(UUID userId, ReportType format, PeriodType period,
                                    OffsetDateTime from, OffsetDateTime to) {
        String fromDate = from.format(KEY_DATE_FORMATTER);
        String toDate = to.format(KEY_DATE_FORMATTER);

        return String.format("report/%s/%s/%s_%s_%s.%s",
                userId,
                format.toString().toLowerCase(),
                period.toString().toLowerCase(),
                fromDate,
                toDate,
                ReportType.EXCEL.equals(format) ? "xlsx" : "html");
    }

    public ReportPeriodParams getPeriodParams(PeriodType period, OffsetDateTime now) {
        return switch (period) {
            case PeriodType.WEEK -> new ReportPeriodParams(
                    now.minusWeeks(1),
                    now,
                    Bucket.DAY
            );
            case PeriodType.MONTH -> new ReportPeriodParams(
                    now.minusMonths(1),
                    now,
                    Bucket.WEEK
            );
            case PeriodType.YEAR -> new ReportPeriodParams(
                    now.minusYears(1),
                    now,
                    Bucket.MONTH
            );
        };
    }

    public boolean isCached(PeriodType period, ReportType format, OffsetDateTime from, OffsetDateTime to) {
        UUID userId = AuthContextHolder.getId();

        return requestRepository.existsByExternalIdAndPeriodAndFormatAndFromDateAndToDate
                (userId, period, format, from, to);
    }

    public OffsetDateTime getLastDateOfUserRequest(ReportType format) {
        UUID userId = AuthContextHolder.getId();

        return requestRepository.findTopByExternalIdAndFormatOrderByToDateDesc(userId, format)
                .orElseThrow(() -> new RequestNotFoundException("Последняя запись запроса по отчёту не найдена"))
                .getToDate();
    }
}
