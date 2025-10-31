package ru.itmo.specification;

import ru.itmo.domain.Human;
import org.springframework.data.jpa.domain.Specification;
import ru.itmo.page.HumanPageRequest;

public final class HumanSpecifications {
    public static Specification<Human> byRequest(HumanPageRequest rq) {
        return Specification.where(idEq(rq.getId()))
                .and(heightEq(rq.getHeight()));
    }
    private static Specification<Human> idEq(Long id) {
        return (root, q, cb) -> id == null ? null : cb.equal(root.get("id"), id);
    }
    private static Specification<Human> heightEq(Float h) {
        return (root, q, cb) -> h == null ? null : cb.equal(root.get("height"), h);
    }
}