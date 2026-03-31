package org.wa.report.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wa.report.service.enumeration.PeriodType;
import org.wa.report.service.enumeration.ReportType;
import org.wa.report.service.model.ReportRequest;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRequestRepository extends JpaRepository<ReportRequest, Long> {
    boolean existsByExternalIdAndPeriodAndFormatAndFromDateAndToDate
            (UUID externalId, PeriodType period, ReportType format, OffsetDateTime fromDate, OffsetDateTime toDate);

    Optional<ReportRequest> findTopByExternalIdAndFormatOrderByToDateDesc(UUID externalId, ReportType format);
}
