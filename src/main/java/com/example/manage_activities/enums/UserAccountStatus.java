package com.example.manage_activities.enums;

import java.util.Locale;

public enum UserAccountStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    UserAccountStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserAccountStatus fromValue(String status) {
        if (status == null) {
            return ACTIVE;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        for (UserAccountStatus accountStatus : values()) {
            if (accountStatus.value.equals(normalized)) {
                return accountStatus;
            }
        }
        throw new IllegalArgumentException("Invalid user status: " + status);
    }
}
