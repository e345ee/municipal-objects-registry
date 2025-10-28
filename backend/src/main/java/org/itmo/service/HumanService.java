package org.itmo.service;

import jakarta.persistence.EntityNotFoundException;
import org.itmo.domain.Human;
import org.itmo.dto.HumanDto;
import org.itmo.repository.HumanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HumanService {
    private final HumanRepository repo;


    public HumanService(HumanRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public HumanDto create(HumanDto humanDto) {
        Human human = humanDto.toNewEntity();

        human = repo.save(human);
        return HumanDto.fromEntity(human);
    }

    @Transactional
    public HumanDto update(Long id, HumanDto humanDto) {
        Human human = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Human Not Found"));
        human = humanDto.toNewEntity();
        human = repo.save(human);
        return HumanDto.fromEntity(human);
    }

    @Transactional(readOnly = true)
    public HumanDto get(Long id) {
        return HumanDto.fromEntity(repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Человек с id=" + id + " не найден")));
    }

    @Transactional(readOnly = true)
    public List<HumanDto> list(){
        return repo.findAll().stream().map(HumanDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        if (repo.existsById(id)) {
            throw new EntityNotFoundException("Human Not Found");
        }
        repo.deleteById(id);
    }
}
