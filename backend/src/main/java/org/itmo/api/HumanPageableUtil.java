package org.itmo.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public final class HumanPageableUtil {
    public static Pageable toPageable(HumanPageRequest rq) {
        List<Sort.Order> orders = new ArrayList<>();
        if (rq.getSort() != null) {
            for (String s : rq.getSort()) {
                if (s == null || s.isBlank()) continue;
                String[] p = s.split(",", 2);
                String field = p[0].trim();
                String dir = (p.length > 1 ? p[1] : "asc").toLowerCase();
                Sort.Direction d = "desc".equals(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
                switch (field) {
                    case "id":
                    case "height":
                        orders.add(new Sort.Order(d, field)); break;
                    default:
                }
            }
        }
        Sort sort = orders.isEmpty() ? Sort.by("id").ascending() : Sort.by(orders);
        return PageRequest.of(
                rq.getPage() == null ? 0 : rq.getPage(),
                rq.getSize() == null ? 20 : rq.getSize(),
                sort
        );
    }
}