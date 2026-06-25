package com.example.manage_activities.repository.projection;

public interface StudentStatisticsProjection {

    String getStudentId();

    String getStudentCode();

    String getFullName();

    String getClassName();

    String getDepartment();

    Long getParticipatedActivityCount();

    Long getTotalEarnedPoints();
}
