package com.example.manage_activities.repository.projection;

import com.example.manage_activities.enums.ActivityStatus;

public interface ActivityStatusCountProjection {

    ActivityStatus getStatus();

    Long getCount();
}
