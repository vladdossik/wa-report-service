package org.wa.report.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class AggregatedMetricDto {
    private OffsetDateTime bucket;
    private Double avgHeartRate;
    private Integer maxHeartRate;
    private Double avgSteps;
    private Integer maxSteps;
    private Double avgSleep;
}
