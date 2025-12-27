package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.*;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {
    private final SkillRepository skillRepository;

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

        return skill.orElse(null);
    }

    @Override
    public ResultPaginationResponse handleGetSkills(Specification<Skill> spec, Pageable pageable) {
        Page<Skill> page = this.skillRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        return new ResultPaginationResponse(meta, page.getContent());
    }

    @Override
    public ResultPaginationResponse handleGetAllSkills(Specification<Skill> spec) {
        List<Skill> allSkills = this.skillRepository.findAll(spec);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(1);
        meta.setPageSize(allSkills.size());
        meta.setPages(1);
        meta.setTotal(allSkills.size());

        return new ResultPaginationResponse(meta, allSkills);
    }

    @Override
    public boolean handleExistSkill(String name) {
        return this.skillRepository.existsByName(name);
    }
}
