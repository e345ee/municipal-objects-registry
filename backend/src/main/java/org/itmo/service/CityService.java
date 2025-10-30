package org.itmo.service;

import jakarta.persistence.EntityManager;
import org.itmo.api.*;
import org.itmo.domain.*;
import org.itmo.dto.CityDto;
import org.itmo.dto.CoordinatesDto;
import org.itmo.dto.HumanDto;
import org.itmo.repository.CityRepository;
import org.itmo.repository.CoordinatesRepository;
import org.itmo.repository.HumanRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CityService {

    private final CityRepository cityRepo;
    private final CoordinatesRepository coordsRepo;
    private final HumanRepository humanRepo;
    private final EntityManager em;

    public CityService(
            CityRepository cityRepo,
            CoordinatesRepository coordsRepo,
            HumanRepository humanRepo,
            EntityManager em
    ) {
        this.cityRepo = cityRepo;
        this.coordsRepo = coordsRepo;
        this.humanRepo = humanRepo;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public PageDto<CityDto> page(CityPageRequest rq) {

        Specification<City> spec = CitySpecifications.byRequest(rq);


        String sortBy = rq.getSortBy();
        String dir    = rq.getDir();
        boolean asc   = !"desc".equalsIgnoreCase(dir);

        if (sortBy != null && !sortBy.isBlank()) {
            Pageable pageableNoSort = PageRequest.of(rq.getPage(), rq.getSize());

            Page<City> page;
            switch (sortBy) {
                case "id":
                    page = asc
                            ? cityRepo.findAllByOrderByIdAsc(pageableNoSort)
                            : cityRepo.findAllByOrderByIdDesc(pageableNoSort);
                    break;

                case "coordinatesId":
                    page = asc
                            ? cityRepo.findAllOrderByCoordinatesIdAsc(pageableNoSort)
                            : cityRepo.findAllOrderByCoordinatesIdDesc(pageableNoSort);
                    break;

                case "governorId":
                    page = asc
                            ? cityRepo.findAllOrderByGovernorIdAsc(pageableNoSort)
                            : cityRepo.findAllOrderByGovernorIdDesc(pageableNoSort);
                    break;

                case "coordinatesX":
                    page = asc
                            ? cityRepo.findAllOrderByCoordinatesXAsc(pageableNoSort)
                            : cityRepo.findAllOrderByCoordinatesXDesc(pageableNoSort);
                    break;

                case "coordinatesY":
                    page = asc
                            ? cityRepo.findAllOrderByCoordinatesYAsc(pageableNoSort)
                            : cityRepo.findAllOrderByCoordinatesYDesc(pageableNoSort);
                    break;

                case "governorHeight":
                    page = asc
                            ? cityRepo.findAllOrderByGovernorHeightAsc(pageableNoSort)
                            : cityRepo.findAllOrderByGovernorHeightDesc(pageableNoSort);
                    break;

                default:
                    Pageable pageable = PageableUtil.toPageable(rq);
                    page = cityRepo.findAll(spec, pageable);
            }

            return PageDto.fromPage(page.map(this::toDto));
        }

        Pageable pageable = PageableUtil.toPageable(rq);
        Page<City> page = cityRepo.findAll(spec, pageable);
        return PageDto.fromPage(page.map(this::toDto));
    }

    @Transactional
    public CityDto create(CityDto dto) {
        if ((dto.getCoordinatesId() == null) == (dto.getCoordinates() == null)) {
            throw new IllegalArgumentException("Provide either coordinatesId OR coordinates (exactly one).");
        }
        if (dto.getGovernorId() != null && dto.getGovernor() != null) {
            throw new IllegalArgumentException("Provide either governorId OR governor, not both.");
        }

        City e = new City();

        e.setName(dto.getName());
        e.setArea(dto.getArea());
        e.setPopulation(dto.getPopulation());
        e.setEstablishmentDate(dto.getEstablishmentDate());
        e.setCapital(dto.getCapital());
        e.setMetersAboveSeaLevel(dto.getMetersAboveSeaLevel());
        e.setTelephoneCode(dto.getTelephoneCode());
        e.setClimate(Climate.valueOf(dto.getClimate()));
        e.setGovernment(Government.valueOf(dto.getGovernment()));

        Coordinates coords = (dto.getCoordinatesId() != null)
                ? coordsRepo.findById(dto.getCoordinatesId())
                .orElseThrow(() -> new RelatedEntityNotFound("Coordinates", dto.getCoordinatesId()))
                : coordsRepo.save(dto.getCoordinates().toNewEntity());
        e.setCoordinates(coords);

        if (dto.getGovernorId() != null) {
            Human gov = humanRepo.findById(dto.getGovernorId())
                    .orElseThrow(() -> new RelatedEntityNotFound("Human", dto.getGovernorId()));
            e.setGovernor(gov);
        } else if (dto.getGovernor() != null) {
            e.setGovernor(humanRepo.save(dto.getGovernor().toNewEntity()));
        } else {
            e.setGovernor(null);
        }

        e = cityRepo.save(e);

        cityRepo.flush();
        em.refresh(e);

        return toDto(e);
    }

    @Transactional
    public CityDto update(Long id, CityDto dto) {
        City e = cityRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Город с id=" + id + " не найден"));

        e.setName(dto.getName());
        e.setArea(dto.getArea());
        e.setPopulation(dto.getPopulation());
        e.setCapital(dto.isCapital());
        e.setMetersAboveSeaLevel(dto.getMetersAboveSeaLevel());
        e.setTelephoneCode(dto.getTelephoneCode());
        e.setClimate(Climate.valueOf(dto.getClimate()));
        e.setGovernment(Government.valueOf(dto.getGovernment()));
        e.setEstablishmentDate(dto.getEstablishmentDate());

        if (dto.isCoordinatesSpecified()) {
            if (dto.getCoordinatesId() == null && dto.getCoordinates() == null) {
                throw new IllegalArgumentException("Coordinates must be provided: either coordinatesId OR coordinates.");
            }
            if (dto.getCoordinatesId() != null && dto.getCoordinates() != null) {
                throw new IllegalArgumentException("Provide either coordinatesId OR coordinates, not both.");
            }
            Coordinates coords = (dto.getCoordinatesId() != null)
                    ? coordsRepo.findById(dto.getCoordinatesId())
                    .orElseThrow(() -> new RelatedEntityNotFound("Coordinates", dto.getCoordinatesId()))
                    : coordsRepo.save(dto.getCoordinates().toNewEntity());
            e.setCoordinates(coords);
        }
        if (dto.isGovernorSpecified()) {
            if (dto.getGovernorId() != null && dto.getGovernor() != null) {
                throw new IllegalArgumentException("Provide either governorId OR governor, not both.");
            }
            if (dto.getGovernorId() != null) {
                e.setGovernor(
                        humanRepo.findById(dto.getGovernorId())
                                .orElseThrow(() -> new RelatedEntityNotFound("Human", dto.getGovernorId()))
                );
            } else if (dto.getGovernor() != null) {
                e.setGovernor(humanRepo.save(dto.getGovernor().toNewEntity()));
            } else {
                e.setGovernor(null);
            }
        }

        e = cityRepo.save(e);

        return toDto(e);
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
            long usage = cityRepo.countByGovernor_Id(governor.getId());
            if (usage == 0) {
                humanRepo.deleteById(governor.getId());
            }
        }
        if (deleteCoordinatesIfOrphan && coords != null) {
            long usage = cityRepo.countByCoordinates_Id(coords.getId());
            if (usage == 0) {
                coordsRepo.deleteById(coords.getId());
            }
        }
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


}
