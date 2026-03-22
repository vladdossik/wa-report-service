package org.wa.report.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.wa.report.service.enumeration.Bucket;
import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
public class ReportPeriodParams {
    private OffsetDateTime from;
    private OffsetDateTime now;
    private Bucket bucket;
}
