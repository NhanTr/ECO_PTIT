package com.example.manage_activities.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RegistrationStatusConverter implements AttributeConverter<RegistrationStatus, String> {

    @Override
    public String convertToDatabaseColumn(RegistrationStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public RegistrationStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RegistrationStatus.from(dbData);
    }
}
