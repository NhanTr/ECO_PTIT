package com.example.manage_activities.enums;

import lombok.Getter;

@Getter
public enum Roles {
    ADMIN(1, "Quản trị hệ thống"),
    MANAGER(2, "Giảng viên / Quản lý"),
    ORGANIZER(3, "BTC/CLB"),
    STUDENT(4, "Sinh viên");

    private final int id;
    private final String displayNameVi;

    Roles(int id, String displayNameVi) {
        this.id = id;
        this.displayNameVi = displayNameVi;
    }

    public static Roles fromId(Integer roleId) {
        if (roleId == null || roleId < 1 || roleId > values().length) {
            return STUDENT;
        }
        return values()[roleId - 1];
    }

    public static String getNameById(Integer roleId) {
        return fromId(roleId).name();
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
