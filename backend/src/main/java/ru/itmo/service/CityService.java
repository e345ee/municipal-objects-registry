package ru.itmo.service;

import ru.itmo.page.CityPageRequest;
import ru.itmo.specification.CitySpecifications;
import ru.itmo.page.PageDto;
import ru.itmo.exeption.RelatedEntityNotFound;
import ru.itmo.domain.City;
import ru.itmo.domain.Climate;
import ru.itmo.domain.Coordinates;
import ru.itmo.domain.Government;
import ru.itmo.domain.Human;
import ru.itmo.websocet.ChangeAction;
import ru.itmo.dto.CityDto;
import ru.itmo.dto.CoordinatesDto;
import ru.itmo.dto.HumanDto;
import ru.itmo.repository.CityRepository;
import ru.itmo.repository.CoordinatesRepository;
import ru.itmo.repository.HumanRepository;
import ru.itmo.websocet.WsEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CityService {

    private final CityRepository cityRepo;
    private final CoordinatesRepository coordsRepo;
    private final HumanRepository humanRepo;
    private final WsEventPublisher ws;

    public CityService(
            CityRepository cityRepo,
            CoordinatesRepository coordsRepo,
            HumanRepository humanRepo,
            WsEventPublisher ws
    ) {
        this.cityRepo = cityRepo;
        this.coordsRepo = coordsRepo;
        this.humanRepo = humanRepo;
        this.ws = ws;
    }

    @Transactional(readOnly = true)
    public PageDto<CityDto> page(CityPageRequest rq) {

        Specification<City> spec = CitySpecifications.byRequest(rq);

        Sort sort = resolveSort(rq);

        boolean hasIdOrder = sort.stream().anyMatch(o -> "id".equals(o.getProperty()));
        if (!hasIdOrder) {
            sort = sort.and(Sort.by("id").ascending());
        }

        int page = rq.getPage() != null ? rq.getPage() : 0;
        int size = rq.getSize() != null ? rq.getSize() : 20;
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<City> p = cityRepo.findAll(spec, pageable);

        return PageDto.fromPage(p.map(this::toDto));
    }

    private Sort resolveSort(CityPageRequest rq) {

        List<String> sortParams = rq.getSort();
        if (sortParams != null && !sortParams.isEmpty() && (rq.getSortBy() == null || rq.getSortBy().isBlank())) {
            List<Sort.Order> orders = new ArrayList<>();
            for (String s : sortParams) {
                if (s == null || s.isBlank()) continue;
                String[] parts = s.split(",", 2);
                String field = parts[0].trim();
                String dir = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
                orders.add("desc".equals(dir) ? Sort.Order.desc(field) : Sort.Order.asc(field));
            }
            if (!orders.isEmpty()) return Sort.by(orders);
        }

        String sortBy = rq.getSortBy();
        String dir = rq.getDir() != null ? rq.getDir().toLowerCase() : "asc";

        String path = mapSortByToPath(sortBy);
        Sort.Order order = "desc".equals(dir) ? Sort.Order.desc(path) : Sort.Order.asc(path);

        order = "desc".equals(dir) ? order.nullsFirst() : order.nullsLast();

        return Sort.by(order);
    }

    private String mapSortByToPath(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return "id";

        switch (sortBy) {

            case "id": return "id";
            case "name": return "name";
            case "creationDate": return "creationDate";
            case "area": return "area";
            case "population": return "population";
            case "establishmentDate": return "establishmentDate";
            case "capital": return "capital";
            case "metersAboveSeaLevel": return "metersAboveSeaLevel";
            case "telephoneCode": return "telephoneCode";
            case "climate": return "climate";
            case "government": return "government";

            case "coordinatesId": return "coordinates.id";
            case "coordinatesX":  return "coordinates.x";
            case "coordinatesY":  return "coordinates.y";
            case "governorId":    return "governor.id";
            case "governorHeight":return "governor.height";

            default: return sortBy;
        }
    }

    @Transactional
    public CityDto create(CityDto dto) {
        if ((dto.getCoordinatesId() == null) == (dto.getCoordinates() == null)) {
            throw new IllegalArgumentException("Provide either coordinatesId OR coordinates (exactly one).");
        }
        if (dto.getGovernorId() != null && dto.getGovernor() != null) {
            throw new IllegalArgumentException("Provide either governorId OR governor, not both.");
        }

        boolean createdNewCoords = false;
        boolean createdNewGovernor = false;

        City e = new City();

        e.setName(dto.getName());
        e.setArea(dto.getArea());
        e.setPopulation(dto.getPopulation());
        e.setEstablishmentDate(dto.getEstablishmentDate());
        e.setCapital(dto.getCapital());
        e.setMetersAboveSeaLevel(dto.getMetersAboveSeaLevel());
        e.setTelephoneCode(dto.getTelephoneCode());
        e.setClimate(Climate.valueOf(dto.getClimate()));
        e.setGovernment(parseEnumOrNull(Government.class, dto.getGovernment()));

        Coordinates coords;
        if (dto.getCoordinatesId() != null) {
            coords = coordsRepo.findById(dto.getCoordinatesId())
                    .orElseThrow(() -> new RelatedEntityNotFound("Coordinates", dto.getCoordinatesId()));
        } else {
            coords = coordsRepo.save(dto.getCoordinates().toNewEntity());
            createdNewCoords = true;
        }
        e.setCoordinates(coords);

        if (dto.getGovernorId() != null) {
            Human gov = humanRepo.findById(dto.getGovernorId())
                    .orElseThrow(() -> new RelatedEntityNotFound("Human", dto.getGovernorId()));
            e.setGovernor(gov);
        } else if (dto.getGovernor() != null) {
            Human gov = humanRepo.save(dto.getGovernor().toNewEntity());
            e.setGovernor(gov);
            createdNewGovernor = true;
        } else {
            e.setGovernor(null);
        }

        e = cityRepo.save(e);
        cityRepo.flush();

        if (createdNewCoords && e.getCoordinates() != null) {
            ws.sendChange("Coordinates", ChangeAction.CREATED, e.getCoordinates().getId(),
                    CoordinatesDto.fromEntity(e.getCoordinates()));
        }
        if (createdNewGovernor && e.getGovernor() != null) {
            ws.sendChange("Human", ChangeAction.CREATED, e.getGovernor().getId(),
                    HumanDto.fromEntity(e.getGovernor()));
        }

        CityDto out = toDto(e);
        ws.sendChange("City", ChangeAction.CREATED, out.getId(), out);

        return out;
    }

    private static <E extends Enum<E>> E parseEnumOrNull(Class<E> type, String v) {
        if (v == null || v.isBlank()) return null;
        return Enum.valueOf(type, v);
    }

    @Transactional
    public CityDto update(Long id, CityDto dto) {
        City e = cityRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Город с id=" + id + " не найден"));

        boolean updatedExistingCoords = false;
        boolean createdNewCoords = false;
        boolean updatedExistingGovernor = false;
        boolean createdNewGovernor = false;

        e.setName(dto.getName());
        e.setArea(dto.getArea());
        e.setPopulation(dto.getPopulation());
        e.setCapital(Boolean.TRUE.equals(dto.getCapital()));
        e.setMetersAboveSeaLevel(dto.getMetersAboveSeaLevel());
        e.setTelephoneCode(dto.getTelephoneCode());
        e.setClimate(Climate.valueOf(dto.getClimate()));

        if (dto.getGovernment() == null || dto.getGovernment().isBlank()) {
            e.setGovernment(null);
        } else {
            e.setGovernment(Government.valueOf(dto.getGovernment()));
        }

        e.setEstablishmentDate(dto.getEstablishmentDate());

        if (dto.isCoordinatesSpecified()) {

            if (dto.getCoordinatesId() != null && dto.getCoordinates() != null) {
                throw new IllegalArgumentException("Provide either coordinatesId OR coordinates, not both.");
            }

            if (dto.getCoordinatesId() != null) {

                Coordinates coords = coordsRepo.findById(dto.getCoordinatesId())
                        .orElseThrow(() -> new RelatedEntityNotFound("Coordinates", dto.getCoordinatesId()));
                e.setCoordinates(coords);

            } else if (dto.getCoordinates() != null) {

                if (e.getCoordinates() != null) {
                    dto.getCoordinates().applyToEntity(e.getCoordinates());
                    coordsRepo.save(e.getCoordinates());
                    updatedExistingCoords = true;
                } else {
                    Coordinates created = coordsRepo.save(dto.getCoordinates().toNewEntity());
                    e.setCoordinates(created);
                    createdNewCoords = true;
                }
            } else {
                throw new IllegalArgumentException("Coordinates must be provided: either coordinatesId OR coordinates.");
            }
        }

        if (dto.isGovernorSpecified()) {
            if (dto.getGovernorId() != null && dto.getGovernor() != null) {
                throw new IllegalArgumentException("Provide either governorId OR governor, not both.");
            }

            if (dto.getGovernorId() != null) {

                Human gov = humanRepo.findById(dto.getGovernorId())
                        .orElseThrow(() -> new RelatedEntityNotFound("Human", dto.getGovernorId()));
                e.setGovernor(gov);
            } else if (dto.getGovernor() != null) {
                if (e.getGovernor() != null) {
                    dto.getGovernor().applyToEntity(e.getGovernor());
                    humanRepo.save(e.getGovernor());
                    updatedExistingGovernor = true;
                } else {
                    Human created = humanRepo.save(dto.getGovernor().toNewEntity());
                    e.setGovernor(created);
                    createdNewGovernor = true;
                }
            } else {
                e.setGovernor(null);
            }
        }

        e = cityRepo.save(e);

        if (updatedExistingCoords && e.getCoordinates() != null) {
            ws.sendChange("Coordinates", ChangeAction.UPDATED, e.getCoordinates().getId(),
                    CoordinatesDto.fromEntity(e.getCoordinates()));
        }
        if (createdNewCoords && e.getCoordinates() != null) {
            ws.sendChange("Coordinates", ChangeAction.CREATED, e.getCoordinates().getId(),
                    CoordinatesDto.fromEntity(e.getCoordinates()));
        }

        if (updatedExistingGovernor && e.getGovernor() != null) {
            ws.sendChange("Human", ChangeAction.UPDATED, e.getGovernor().getId(),
                    HumanDto.fromEntity(e.getGovernor()));
        }
        if (createdNewGovernor && e.getGovernor() != null) {
            ws.sendChange("Human", ChangeAction.CREATED, e.getGovernor().getId(),
                    HumanDto.fromEntity(e.getGovernor()));
        }

        CityDto out = toDto(e);
        ws.sendChange("City", ChangeAction.UPDATED, out.getId(), out);
        return out;
    }

    @Transactional(readOnly = true)
    public CityDto get(Long id) {
        return toDto(cityRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Город с id=" + id + " не найден")));
    }

    @Transactional(readOnly = true)
    public List<CityDto> list() {
        return cityRepo.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public void delete(Long id,
                       boolean deleteGovernorIfOrphan,
                       boolean deleteCoordinatesIfOrphan) {
        City city = cityRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Город с id=" + id + " не найден"));

        Human governor = city.getGovernor();
        Coordinates coords = city.getCoordinates();

        cityRepo.delete(city);
        cityRepo.flush();

        if (deleteGovernorIfOrphan && governor != null) {
            long usage = cityRepo.countByGovernorId(governor.getId());
            if (usage == 0) {
                humanRepo.deleteById(governor.getId());
                ws.sendChange("Human", ChangeAction.DELETED, governor.getId(), null);
            }
        }
        if (deleteCoordinatesIfOrphan && coords != null) {
            long usage = cityRepo.countByCoordinatesId(coords.getId());
            if (usage == 0) {
                coordsRepo.deleteById(coords.getId());
                ws.sendChange("Coordinates", ChangeAction.DELETED, coords.getId(), null);
            }
        }
        ws.sendChange("City", ChangeAction.DELETED, id, null);
    }

    private CityDto toDto(City e) {
        CityDto dto = new CityDto();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setArea(e.getArea());
        dto.setPopulation(e.getPopulation());
        dto.setCapital(e.isCapital());
        dto.setMetersAboveSeaLevel(e.getMetersAboveSeaLevel());
        dto.setTelephoneCode(e.getTelephoneCode());
        dto.setClimate(e.getClimate() != null ? e.getClimate().name() : null);
        dto.setGovernment(e.getGovernment() != null ? e.getGovernment().name() : null);

        dto.setCreationDate(e.getCreationDate());
        dto.setEstablishmentDate(e.getEstablishmentDate());

        dto.setCoordinates(e.getCoordinates() != null ? CoordinatesDto.fromEntity(e.getCoordinates()) : null);
        dto.setGovernor(e.getGovernor() != null ? HumanDto.fromEntity(e.getGovernor()) : null);

        return dto;
    }

    @Transactional(readOnly = true)
    public double averageTelephoneCode() {
        var codes = cityRepo.findAll().stream()
                .map(City::getTelephoneCode)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .toArray();
        if (codes.length == 0) return 0.0d;
        long sum = 0;
        for (int c : codes) sum += c;
        return (double) sum / codes.length;
    }

    @Transactional(readOnly = true)
    public List<CityDto> findByNameStartsWith(String prefix) {
        if (prefix == null) prefix = "";
        final String p = prefix;
        return cityRepo.findAll().stream()
                .filter(c -> c.getName() != null && c.getName().startsWith(p))
                .map(CityDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Integer> uniqueMetersAboveSeaLevel() {
        return cityRepo.findAll().stream()
                .map(City::getMetersAboveSeaLevel)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> new TreeSet<Integer>()))
                .stream()
                .toList();
    }

    @Transactional(readOnly = true)
    public double distanceToLargestAreaCity(double fromX, double fromY) {
        Optional<City> maxAreaCity = cityRepo.findAll().stream()
                .filter(c -> c.getArea() != null)
                .max(Comparator.comparingInt(City::getArea));

        City city = maxAreaCity.orElseThrow(() -> new NoSuchElementException("Нет городов с заполненным area"));
        if (city.getCoordinates() == null)
            throw new NoSuchElementException("У города с max area отсутствуют координаты");

        double dx = city.getCoordinates().getX() - fromX;
        double dy = city.getCoordinates().getY() - fromY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Transactional(readOnly = true)
    public double distanceFromOriginToOldestCity() {
        Optional<City> oldest = cityRepo.findAll().stream()
                .filter(c -> c.getEstablishmentDate() != null)
                .min(Comparator.comparing(City::getEstablishmentDate));

        City city = oldest.orElseThrow(() -> new NoSuchElementException("Нет городов с establishmentDate"));
        if (city.getCoordinates() == null)
            throw new NoSuchElementException("У самого старого города отсутствуют координаты");

        double x = city.getCoordinates().getX();
        double y = city.getCoordinates().getY();
        return Math.sqrt(x * x + y * y);
    }
}
