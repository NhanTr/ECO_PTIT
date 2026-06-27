package com.example.manage_activities.enums;

import java.util.Arrays;

public enum ActivityStatus {
    DRAFT("Draft"),
    PENDING("Pending"),
    REVIEWING("Reviewing"),
    CANCELLATION_REQUESTED("CancellationRequested"),
    APPROVED("Approved"),
    ONGOING("Ongoing"),
    CLOSED("Closed"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    private final String value;

    ActivityStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ActivityStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if ("completed".equalsIgnoreCase(normalized)) {
            return CLOSED;
        }
        if ("upcoming".equalsIgnoreCase(normalized)) {
            return APPROVED;
        }
        if ("cancellation_requested".equalsIgnoreCase(normalized)
                || "cancellation requested".equalsIgnoreCase(normalized)
                || "cancelrequested".equalsIgnoreCase(normalized)
                || "cancel requested".equalsIgnoreCase(normalized)) {
            return CANCELLATION_REQUESTED;
        }
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(normalized)
                        || status.value.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid activity status: " + value));
    }
}
