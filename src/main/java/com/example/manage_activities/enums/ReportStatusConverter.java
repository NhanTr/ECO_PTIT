package com.example.manage_activities.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReportStatusConverter implements AttributeConverter<ReportStatus, String> {

    @Override
    public String convertToDatabaseColumn(ReportStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ReportStatus convertToEntityAttribute(String dbData) {
        return dbData == null || dbData.isBlank() ? null : ReportStatus.from(dbData);
    }
}
