package org.wa.report.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class AggregatedActivityDto {
    private OffsetDateTime bucket;
    private String eventType;
    private String unit;
    private Long eventCount;
    private Integer totalQuantity;
    private Double avgQuantity;
    private Integer minQuantity;
    private Integer maxQuantity;
}
