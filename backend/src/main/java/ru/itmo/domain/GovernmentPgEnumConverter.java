package ru.itmo.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

@Converter(autoApply = true)
public class GovernmentPgEnumConverter implements AttributeConverter<Government, Object> {

    @Override
    public Object convertToDatabaseColumn(Government attribute) {
        try {
            PGobject pg = new PGobject();
            pg.setType("government");

            pg.setValue(attribute == null ? null : attribute.name());
            return pg;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert Government to PGobject", e);
        }
    }

    @Override
    public Government convertToEntityAttribute(Object dbData) {
        if (dbData == null) return null;
        String v = dbData.toString();
        return (v == null || v.isBlank()) ? null : Government.valueOf(v);
    }
}