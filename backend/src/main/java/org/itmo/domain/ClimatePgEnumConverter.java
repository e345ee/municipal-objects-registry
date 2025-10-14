package org.itmo.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

@Converter(autoApply = true)
public class ClimatePgEnumConverter implements AttributeConverter<Climate, Object> {

    @Override
    public Object convertToDatabaseColumn(Climate attribute) {
        if (attribute == null) return null;
        try {
            PGobject pg = new PGobject();
            pg.setType("climate");           // точное имя типа в БД
            pg.setValue(attribute.name());   // RAIN_FOREST | HUMIDSUBTROPICAL | TUNDRA
            return pg;                       // драйвер пошлёт параметр типа OTHER(enum)
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Climate convertToEntityAttribute(Object dbData) {
        return dbData == null ? null : Climate.valueOf(dbData.toString());
    }
}