package ru.itmo.specification;

import jakarta.persistence.criteria.JoinType;
import ru.itmo.domain.City;
import ru.itmo.domain.Climate;
import ru.itmo.domain.Government;
import org.springframework.data.jpa.domain.Specification;
import ru.itmo.page.CityPageRequest;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;


import java.time.LocalDate;


public final class CitySpecifications {

    private CitySpecifications() {}

    public static Specification<City> byRequest(CityPageRequest rq) {
        return Specification
                .where(eqLong("id", rq.getId()))
                .and(containsIgnoreCase("name", rq.getName()))
                .and(eqEnumName("climate", rq.getClimate(), Climate.class))
                .and(eqEnumName("government", rq.getGovernment(), Government.class))
                .and(eqLong("population", rq.getPopulation()))
                .and(eqInteger("telephoneCode", rq.getTelephoneCode()))
                .and(eqBoolean("capital", rq.getCapital()))
                .and(eqLong("area", rq.getArea()))
                .and(eqLong("metersAboveSeaLevel", rq.getMetersAboveSeaLevel()))
                .and(nestedEq("coordinates", "id", rq.getCoordinatesId()))
                .and(nestedEq("governor", "id", rq.getGovernorId()))
                .and(nestedNullCheck("governor", "id", rq.getGovernorIdIsNull()))
                .and(eqDate("creationDate", rq.getCreationDate()))
                .and(eqDate("establishmentDate", rq.getEstablishmentDate()));
    }


    private static Specification<City> eqDate(String field, java.time.LocalDate date) {
        if (date == null) return null;
        return (root, q, cb) -> {
            Class<?> attrType = root.get(field).getJavaType();


            if (java.sql.Date.class.isAssignableFrom(attrType) || java.util.Date.class.isAssignableFrom(attrType)) {
                return cb.equal(root.get(field), java.sql.Date.valueOf(date));
            }


            if (java.time.LocalDate.class.isAssignableFrom(attrType)) {
                return cb.equal(root.get(field), date);
            }


            return cb.equal(root.get(field), java.sql.Date.valueOf(date));
        };
    }

    private static Specification<City> containsIgnoreCase(String field, String value) {
        if (value == null || value.isBlank()) return null;
        return (root, q, cb) -> cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%");
    }

    private static <E extends Enum<E>> Specification<City> eqEnumName(String field, String value, Class<E> enumClass) {
        if (value == null || value.isBlank()) return null;
        E enumVal = Enum.valueOf(enumClass, value.trim().toUpperCase(Locale.ROOT));
        return (root, q, cb) -> cb.equal(root.get(field), enumVal);
    }

    private static Specification<City> nestedEq(String assoc, String nestedField, Object val) {
        if (val == null) return null;
        return (root, q, cb) -> cb.equal(root.join(assoc, JoinType.LEFT).get(nestedField), val);
    }

    private static Specification<City> nestedNullCheck(String assoc, String nestedField, Boolean isNull) {
        if (isNull == null) return null;
        return (root, q, cb) -> isNull
                ? cb.isNull(root.join(assoc, JoinType.LEFT).get(nestedField))
                : cb.isNotNull(root.join(assoc, JoinType.LEFT).get(nestedField));
    }

    private static Specification<City> eqLong(String field, Long val) {
        if (val == null) return null;
        return (root, q, cb) -> cb.equal(root.get(field), val);
    }

    private static Specification<City> eqInteger(String field, Integer val) {
        if (val == null) return null;
        return (root, q, cb) -> cb.equal(root.get(field), val);
    }

    private static Specification<City> eqBoolean(String field, Boolean val) {
        if (val == null) return null;
        return (root, q, cb) -> cb.equal(root.get(field), val);
    }

    private static Specification<City> eqLocalDate(String field, LocalDate date) {
        if (date == null) return null;
        return (root, q, cb) -> cb.equal(root.get(field), date);
    }



    @SuppressWarnings("unused")
    private static Climate parseClimate(String v) {
        for (Climate c : Climate.values()) {
            if (c.name().equalsIgnoreCase(v)) return c;
        }
        throw new IllegalArgumentException("Unknown climate: " + v + ". Allowed: " + allowedList(Climate.values()));
    }

    @SuppressWarnings("unused")
    private static Government parseGovernment(String v) {
        for (Government g : Government.values()) {
            if (g.name().equalsIgnoreCase(v)) return g;
        }
        throw new IllegalArgumentException("Unknown government: " + v + ". Allowed: " + allowedList(Government.values()));
    }

    private static String allowedList(Enum<?>[] values) {
        return Arrays.stream(values)
                .map(e -> e.name().toUpperCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
    }
}
