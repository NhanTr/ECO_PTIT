package com.example.manage_activities.enums;

import java.util.Arrays;

public enum RegistrationStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    private final String value;

    RegistrationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RegistrationStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if ("registered".equalsIgnoreCase(normalized)) {
            return PENDING;
        }
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(normalized)
                        || status.value.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid registration status: " + value));
    }
}
