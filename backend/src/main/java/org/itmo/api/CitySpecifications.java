package org.itmo.api;

import jakarta.persistence.criteria.JoinType;
import org.itmo.domain.City;
import org.itmo.domain.Climate;
import org.itmo.domain.Government;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public final class CitySpecifications {

    private CitySpecifications() {}

    public static Specification<City> byRequest(CityPageRequest rq) {
        return Specification.where(idEq(rq.getId()))
                .and(coordinatesIdEq(rq.getCoordinatesId()))
                .and(governorFilter(rq.getGovernorId(), rq.getGovernorIdIsNull()))
                .and(nameEq(rq.getName()))
                .and(climateEq(rq.getClimate()))
                .and(governmentEq(rq.getGovernment()))
                .and(populationEq(rq.getPopulation()))
                .and(telephoneCodeEq(rq.getTelephoneCode()));
    }




    private static Specification<City> idEq(Long id) {
        return (root, q, cb) -> (id == null) ? null : cb.equal(root.get("id"), id);
    }

    private static Specification<City> coordinatesIdEq(Long coordId) {
        return (root, q, cb) -> (coordId == null) ? null
                : cb.equal(root.join("coordinates", JoinType.LEFT).get("id"), coordId);
    }


    private static Specification<City> governorFilter(Long governorId, Boolean isNull) {
        if (Boolean.TRUE.equals(isNull)) {
            return (root, q, cb) -> cb.isNull(root.get("governor"));
        }
        if (governorId != null) {
            return (root, q, cb) ->
                    cb.equal(root.join("governor", JoinType.LEFT).get("id"), governorId);
        }
        return null;
    }




    private static Specification<City> nameEq(String name) {
        return (root, q, cb) -> (name == null) ? null : cb.equal(root.get("name"), name);
    }

    private static Specification<City> climateEq(String climate) {
        if (climate == null) return null;
        Climate parsed = parseClimate(climate);
        return (root, q, cb) -> cb.equal(root.get("climate"), parsed);
    }

    private static Specification<City> governmentEq(String government) {
        if (government == null) return null;
        Government parsed = parseGovernment(government);
        return (root, q, cb) -> cb.equal(root.get("government"), parsed);
    }




    private static Specification<City> populationEq(Long population) {
        return (root, q, cb) -> (population == null) ? null : cb.equal(root.get("population"), population);
    }

    private static Specification<City> telephoneCodeEq(Integer tel) {
        return (root, q, cb) -> (tel == null) ? null : cb.equal(root.get("telephoneCode"), tel);
    }




    private static Climate parseClimate(String v) {
        for (Climate c : Climate.values()) {
            if (c.name().equalsIgnoreCase(v)) return c;
        }
        throw new IllegalArgumentException("Unknown climate: " + v + ". Allowed: " + allowedList(Climate.values()));
    }

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
