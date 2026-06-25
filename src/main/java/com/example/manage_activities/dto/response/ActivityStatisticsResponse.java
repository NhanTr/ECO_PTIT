package com.example.manage_activities.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

/**
 * QLHĐ_BM 2 — báo cáo thống kê hoạt động trong khoảng thời gian.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActivityStatisticsResponse {

    LocalDateTime fromTime;
    LocalDateTime toTime;

    Long totalActivities;
    Long draftCount;
    Long pendingCount;
    Long reviewingCount;
    Long approvedCount;
    Long ongoingCount;
    Long closedCount;
    Long rejectedCount;
    Long cancelledCount;

    List<ActivityStatisticsItemResponse> activities;
}
