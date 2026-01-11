package ru.itmo.service;

import jakarta.persistence.EntityNotFoundException;
import ru.itmo.exception.DeletionBlockedException;
import ru.itmo.dto.HumanPageDto;
import ru.itmo.specification.HumanSpecifications;
import ru.itmo.dto.CityPageDto;
import ru.itmo.domain.Human;
import ru.itmo.websocket.ChangeAction;
import ru.itmo.dto.HumanDto;
import ru.itmo.repository.HumanRepository;
import ru.itmo.websocket.WsEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
public class HumanService {

    private final HumanRepository humanRepo;
    private final WsEventPublisher ws;

    public HumanService(HumanRepository humanRepo,
                        WsEventPublisher ws) {
        this.humanRepo = humanRepo;
        this.ws = ws;
    }

    @Transactional(readOnly = true)
    public CityPageDto<HumanDto> page(HumanPageDto rq,
                                      org.springframework.data.domain.Pageable pageable) {
        Specification<Human> spec = HumanSpecifications.byRequest(rq);
        return CityPageDto.fromPage(humanRepo.findAll(spec, pageable).map(HumanDto::fromEntity));
    }

    @Transactional
    public HumanDto create(HumanDto humanDto) {
        Human human = humanDto.toNewEntity();
        human = humanRepo.save(human);   // этого достаточно

        HumanDto dto = HumanDto.fromEntity(human);
        ws.sendChange("Human", ChangeAction.CREATED, dto.getId(), dto);

        return dto;
    }

    @Transactional
    public HumanDto update(Long id, HumanDto humanDto) {
        Human e = humanRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Human Not Found"));

        humanDto.applyToEntity(e);

        HumanDto dto = HumanDto.fromEntity(e);
        ws.sendChange("Human", ChangeAction.UPDATED, dto.getId(), dto);

        return dto;
    }

    @Transactional(readOnly = true)
    public HumanDto get(Long id) {
        return HumanDto.fromEntity(
                humanRepo.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Человек с id=" + id + " не найден"))
        );
    }

    @Transactional(readOnly = true)
    public List<HumanDto> list() {
        return humanRepo.findAll()
                .stream()
                .map(HumanDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long humanId) {
        if (!humanRepo.existsById(humanId)) {
            throw new NoSuchElementException("Human id=" + humanId + " не найден");
        }

        long usage = humanRepo.countCityUsageByGovernorId(humanId);
        if (usage > 0) {
            List<Long> cityIds = humanRepo.findCityIdsByGovernorId(humanId);
            throw new DeletionBlockedException("Human", humanId, usage, cityIds);
        }

        ws.sendChange("Human", ChangeAction.DELETED, humanId, null);
        humanRepo.deleteById(humanId);
    }

    @Transactional(readOnly = true)
    public Human getEntity(Long id) {
        return humanRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Human Not Found"));
    }

    @Transactional
    public Human saveEntity(Human human) {
        return humanRepo.save(human);
    }

    @Transactional
    public void deleteIfOrphan(Long humanId) {
        long usage = humanRepo.countCityUsageByGovernorId(humanId);
        if (usage == 0) {
            ws.sendChange("Human", ChangeAction.DELETED, humanId, null);
            humanRepo.deleteById(humanId);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Human> findById(Long id) {
        return humanRepo.findById(id);
    }

    @Transactional
    public Human save(Human human) {
        return humanRepo.save(human);
    }

    @Transactional
    public void deleteById(Long id) {
        humanRepo.deleteById(id);
    }
}
