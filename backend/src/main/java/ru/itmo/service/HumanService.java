package ru.itmo.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import ru.itmo.exeption.DeletionBlockedException;
import ru.itmo.page.HumanPageRequest;
import ru.itmo.specification.HumanSpecifications;
import ru.itmo.page.PageDto;
import ru.itmo.domain.Human;
import ru.itmo.websocet.ChangeAction;
import ru.itmo.dto.HumanDto;
import ru.itmo.repository.CityRepository;
import ru.itmo.repository.HumanRepository;
import ru.itmo.websocet.WsEventPublisher;
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
    private final WsEventPublisher ws;

    @PersistenceContext
    private EntityManager em;

    public HumanService(HumanRepository humanRepo,
                        CityRepository cityRepo,
                        WsEventPublisher ws) {
        this.humanRepo = humanRepo;
        this.cityRepo = cityRepo;
        this.ws = ws;
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

        ws.sendChange("Human", ChangeAction.CREATED, HumanDto.fromEntity(human).getId(),HumanDto.fromEntity(human));

        return HumanDto.fromEntity(human);
    }

    @Transactional
    public HumanDto update(Long id, HumanDto humanDto) {
        Human e = humanRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Human Not Found"));

        humanDto.applyToEntity(e);

        ws.sendChange("Human", ChangeAction.UPDATED, HumanDto.fromEntity(e).getId(), HumanDto.fromEntity(e));

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
        ws.sendChange("Human", ChangeAction.DELETED, humanId, null);
        humanRepo.deleteById(humanId);
    }
}
