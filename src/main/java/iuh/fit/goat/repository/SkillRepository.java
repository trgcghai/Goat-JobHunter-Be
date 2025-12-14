package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long>, JpaSpecificationExecutor<Skill> {
//    boolean existsByName(String name);
//    List<Skill> findBySkillIdIn(List<Long> skillIds);
}
