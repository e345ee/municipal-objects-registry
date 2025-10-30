package org.itmo.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public final class PageableUtil {
    private PageableUtil() {}

    public static Pageable toPageable(CityPageRequest rq) {
        int page = rq.getPage() != null ? rq.getPage() : 0;
        int size = rq.getSize() != null ? rq.getSize() : 20;

        List<Sort.Order> orders = new ArrayList<>();


        if (rq.getSort() != null && !rq.getSort().isEmpty()) {
            for (String raw : rq.getSort()) {
                if (raw == null) continue;
                String s = raw.trim();
                if (s.isEmpty()) continue;

                boolean descPrefix = s.startsWith("-");
                if (descPrefix) s = s.substring(1);

                String[] parts = s.split(",", 2);
                String field = parts[0].trim();
                String dir = parts.length > 1
                        ? parts[1].trim().toLowerCase()
                        : (descPrefix ? "desc" : "asc");

                switch (field) {
                    case "id":
                    case "name":
                    case "area":
                    case "population":
                    case "creationDate":
                    case "telephoneCode":
                    case "metersAboveSeaLevel":
                    case "climate":
                    case "government":
                        orders.add("desc".equals(dir)
                                ? Sort.Order.desc(field)
                                : Sort.Order.asc(field));
                        break;
                    default:

                        break;
                }
            }
        }


        Sort sort = orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
        return PageRequest.of(page, size, sort);
    }
}