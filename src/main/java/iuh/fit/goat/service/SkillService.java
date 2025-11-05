package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Skill;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface SkillService {
    Skill handleCreateSkill(Skill skill);

    Skill handleUpdateSkill(Skill skill);

    void handleDeleteSkill(long id);

    Skill handGetSkillById(long id);

    ResultPaginationResponse handleGetAllSkills(Specification<Skill> spec, Pageable pageable);

    boolean handleExistSkill(String name);
}
