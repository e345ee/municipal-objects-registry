package org.itmo.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public final class CoordinatesPageableUtil {
    private CoordinatesPageableUtil() {}

    public static Pageable toPageable(CoordinatesPageRequest rq) {
        List<Sort.Order> orders = new ArrayList<>();
        if (rq.getSort() != null) {
            for (String s : rq.getSort()) {
                if (s == null || s.isBlank()) continue;
                String[] parts = s.split(",", 2);
                String field = parts[0].trim();
                String dir = (parts.length > 1 ? parts[1] : "asc").trim().toLowerCase();
                Sort.Direction direction = "desc".equals(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;


                switch (field) {
                    case "id":
                    case "x":
                    case "y":
                        orders.add(new Sort.Order(direction, field));
                        break;
                    default:

                }
            }
        }
        Sort sort = orders.isEmpty() ? Sort.by("id").ascending() : Sort.by(orders);
        int page = rq.getPage() == null ? 0 : rq.getPage();
        int size = rq.getSize() == null ? 20 : rq.getSize();
        return PageRequest.of(page, size, sort);
    }
}