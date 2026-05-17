package com.example.manage_activities.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ActivityStatusConverter implements AttributeConverter<ActivityStatus, String> {

    @Override
    public String convertToDatabaseColumn(ActivityStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ActivityStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ActivityStatus.from(dbData);
    }
}
