package ru.itmo.specification;

import ru.itmo.domain.Coordinates;
import org.springframework.data.jpa.domain.Specification;
import ru.itmo.page.CoordinatesPageRequest;

public final class CoordinatesSpecifications {
    private CoordinatesSpecifications() {}

    public static Specification<Coordinates> byRequest(
                CoordinatesPageRequest rq
    ) {
        return Specification.where(idEq(rq.getId()))
                .and(xEq(rq.getX()))
                .and(yEq(rq.getY()));
    }

    private static Specification<Coordinates> idEq(Long id) {
        return (root, q, cb) -> id == null ? null : cb.equal(root.get("id"), id);
    }

    private static Specification<Coordinates> xEq(Float x) {
        return (root, q, cb) -> x == null ? null : cb.equal(root.get("x"), x);
    }

    private static Specification<Coordinates> yEq(Float y) {
        return (root, q, cb) -> y == null ? null : cb.equal(root.get("y"), y);
    }
}