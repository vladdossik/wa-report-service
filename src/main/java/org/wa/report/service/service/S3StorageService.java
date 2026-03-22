package org.wa.report.service.service;

import org.springframework.core.io.Resource;
import org.wa.report.service.enumeration.PeriodType;
import org.wa.report.service.enumeration.ReportType;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface S3StorageService {
    String saveReport(UUID userId, ReportType format, PeriodType period,
                      OffsetDateTime from, OffsetDateTime to,
                      byte[] content, String contentType);

    Optional<Resource> getReport(String key);

    boolean reportExists(String key);

    void deleteOldReports(Duration olderThan);

    void deleteReport(String key);

    String getPresignedUrl(String key);
}
