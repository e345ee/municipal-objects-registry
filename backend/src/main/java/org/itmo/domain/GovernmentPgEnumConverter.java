package org.itmo.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

@Converter(autoApply = true)
public class GovernmentPgEnumConverter implements AttributeConverter<Government, Object> {

    @Override
    public Object convertToDatabaseColumn(Government attribute) {
        if (attribute == null) return null;
        try {
            PGobject pg = new PGobject();
            pg.setType("government");
            pg.setValue(attribute.name());
            return pg;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Government convertToEntityAttribute(Object dbData) {
        return dbData == null ? null : Government.valueOf(dbData.toString());
    }
}