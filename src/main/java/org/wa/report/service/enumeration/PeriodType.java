package org.wa.report.service.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PeriodType {
    WEEK("week"),
    MONTH("month"),
    YEAR("year");

    private final String value;
}
