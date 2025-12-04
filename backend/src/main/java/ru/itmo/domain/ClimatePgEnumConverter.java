package ru.itmo.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;
import ru.itmo.domain.Climate;

@Converter(autoApply = true)
public class ClimatePgEnumConverter implements AttributeConverter<Climate, Object> {

    @Override
    public Object convertToDatabaseColumn(Climate attribute) {
        if (attribute == null) return null;
        try {
            PGobject pg = new PGobject();
            pg.setType("climate");
            pg.setValue(attribute.name());
            return pg;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Climate convertToEntityAttribute(Object dbData) {
        return dbData == null ? null : Climate.valueOf(dbData.toString());
    }
}