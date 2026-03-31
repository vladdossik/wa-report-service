package org.wa.report.service.service;

import org.wa.report.service.enumeration.PeriodType;
import org.wa.report.service.enumeration.ReportType;
import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface S3StorageService {
    String saveReport(UUID userId, ReportType format, PeriodType period,
                      OffsetDateTime from, OffsetDateTime to,
                      InputStream contentStream, String contentType, long contentLength);

    boolean reportExists(String key);

    void deleteOldReports(Duration olderThan);

    void deleteReport(String key);

    String getPresignedUrl(String key);
}
