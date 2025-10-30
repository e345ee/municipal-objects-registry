package org.itmo.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.itmo.api.*;
import org.itmo.domain.Coordinates;
import org.itmo.domain.Human;
import org.itmo.dto.HumanDto;
import org.itmo.repository.CityRepository;
import org.itmo.repository.HumanRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.jaxb.SpringDataJaxb;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class HumanService {
    private final HumanRepository humanRepo;
    private final CityRepository cityRepo;

    @PersistenceContext
    private EntityManager em;

    public HumanService(HumanRepository humanRepo, CityRepository cityRepo) {
        this.humanRepo = humanRepo;
        this.cityRepo = cityRepo;
    }


    @Transactional(readOnly = true)
    public PageDto<HumanDto> page(HumanPageRequest rq,
                                  org.springframework.data.domain.Pageable pageable) {
        Specification<Human> spec = HumanSpecifications.byRequest(rq);
        return PageDto.fromPage(humanRepo.findAll(spec, pageable).map(HumanDto::fromEntity));
    }

    @Transactional
    public HumanDto create(HumanDto humanDto) {
        Human human = humanDto.toNewEntity();
        human = humanRepo.save(human);
        humanRepo.flush();
        em.refresh(human);

        return HumanDto.fromEntity(human);
    }

    @Transactional
    public HumanDto update(Long id, HumanDto humanDto) {
        Human e = humanRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Human Not Found"));

        humanDto.applyToEntity(e);

        return HumanDto.fromEntity(e);
    }

    @Transactional(readOnly = true)
    public HumanDto get(Long id) {
        return HumanDto.fromEntity(humanRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Человек с id=" + id + " не найден")));
    }

    @Transactional(readOnly = true)
    public List<HumanDto> list(){
        return humanRepo.findAll().stream().map(HumanDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long humanId) {
        if (!humanRepo.existsById(humanId)) {
            throw new NoSuchElementException("Human id=" + humanId + " не найден");
        }
        long usage = cityRepo.countByGovernor_Id(humanId);
        if (usage > 0) {
            List<Long> cityIds = cityRepo.findIdsByGovernorId(humanId);
            throw new DeletionBlockedException("Human", humanId, usage, cityIds);
        }
        humanRepo.deleteById(humanId);
    }
}
