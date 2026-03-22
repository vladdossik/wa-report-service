package org.wa.report.service.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Bucket {
    DAY("1 day"),
    WEEK("1 week"),
    MONTH("1 month");

    private final String value;
}
