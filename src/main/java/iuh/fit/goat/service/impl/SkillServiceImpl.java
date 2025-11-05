package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.util.*;
import iuh.fit.goat.dto.*;

import java.util.Optional;

@Service
public class SkillServiceImpl implements iuh.fit.goat.service.SkillService {
    private final SkillRepository skillRepository;

    public SkillServiceImpl(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Override
    public Skill handleCreateSkill(Skill skill) {
        return this.skillRepository.save(skill);
    }

    @Override
    public Skill handleUpdateSkill(Skill skill) {
        Skill currentSkill = this.handGetSkillById(skill.getSkillId());

        if(currentSkill != null) {
            currentSkill.setName(skill.getName());
            return this.skillRepository.save(currentSkill);
        }
        return null;
    }

    @Override
    public void handleDeleteSkill(long id) {
        Skill currentSkill = this.handGetSkillById(id);

        if(currentSkill.getJobs() != null) {
            currentSkill.getJobs().forEach(job -> job.getSkills().remove(currentSkill));
        }

        if(currentSkill.getSubscribers() != null){
            currentSkill.getSubscribers().forEach(sub -> sub.getSkills().remove(currentSkill));
        }

        this.skillRepository.deleteById(currentSkill.getSkillId());
    }

    @Override
    public Skill handGetSkillById(long id) {
        Optional<Skill> skill = this.skillRepository.findById(id);

        if(skill.isPresent()) {
            return skill.get();
        }
        return null;
    }

    @Override
    public ResultPaginationResponse handleGetAllSkills(Specification<Skill> spec, Pageable pageable) {
        Page<Skill> page = this.skillRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        return new ResultPaginationResponse(meta, page.getContent());
    }

    @Override
    public boolean handleExistSkill(String name) {
        return this.skillRepository.existsByName(name);
    }
}
