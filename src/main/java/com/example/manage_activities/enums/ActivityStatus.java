package com.example.manage_activities.enums;

import java.util.Arrays;

public enum ActivityStatus {
    DRAFT("Draft"),
    PENDING("Pending"),
    REVIEWING("Reviewing"),
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
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(value)
                        || status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid activity status: " + value));
    }
}
