package com.example.manage_activities.enums;

import java.util.Arrays;

public enum ReportStatus {
    NOT_SUBMITTED("NotSubmitted"),
    PENDING("Pending"),
    REVIEWING("Reviewing"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    private final String value;

    ReportStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ReportStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if ("not_submitted".equalsIgnoreCase(normalized) || "not submitted".equalsIgnoreCase(normalized)) {
            return NOT_SUBMITTED;
        }
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(normalized)
                        || status.value.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid report status: " + value));
    }
}
