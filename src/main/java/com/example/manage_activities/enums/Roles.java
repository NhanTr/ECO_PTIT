package com.example.manage_activities.enums;

public enum Roles {
    ADMIN,
    MANAGER,
    ORGANIZER,
    STUDENT;
    
    public static String getNameById(Integer roleId) {
        if (roleId == null || roleId <= 0) return STUDENT.name();
        return Roles.values()[Math.min(roleId - 1, values().length - 1)].name();
    }
}
