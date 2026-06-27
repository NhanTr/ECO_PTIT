package com.example.manage_activities.enums;

import lombok.Getter;

@Getter
public enum CategoryType {
    DEPARTMENT("Khoa/Lớp"),
    ACTIVITY_TYPE("Loại hoạt động"),
    POINT_TYPE("Loại điểm"),
    SPONSOR("Đối tác/Nhà tài trợ");

    private final String displayNameVi;

    CategoryType(String displayNameVi) {
        this.displayNameVi = displayNameVi;
    }

    public static boolean isValid(String type) {
        if (type == null) return false;
        for (CategoryType t : values()) {
            if (t.name().equalsIgnoreCase(type)) return true;
        }
        return false;
    }
}