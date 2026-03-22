package org.wa.report.service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.wa.report.service.enumeration.Bucket;
import org.wa.report.service.enumeration.PeriodType;
import org.wa.report.service.enumeration.ReportType;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "requests")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportRequest {
    @Id
    @SequenceGenerator(name = "request_seq", sequenceName = "request_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "request_seq")
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true, updatable = false)
    private UUID externalId;

    @Column(name = "period")
    @Enumerated(EnumType.STRING)
    private PeriodType period;

    @Column(name = "format")
    @Enumerated(EnumType.STRING)
    private ReportType format;

    @Column(name = "from_date")
    private OffsetDateTime fromDate;

    @Column(name = "to_date")
    private OffsetDateTime toDate;

    @Column(name = "bucket")
    private Bucket bucket;

    @CreationTimestamp
    @Column(name = "requested_at")
    private OffsetDateTime requestedAt;

    @Column(name = "from_cache")
    private Boolean fromCache;

    @Column(name = "report_key")
    private String reportKey;
}

