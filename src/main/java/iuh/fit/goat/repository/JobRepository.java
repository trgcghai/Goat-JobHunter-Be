package iuh.fit.goat.repository;

import iuh.fit.goat.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
//    List<Job> findByRecruiter(Recruiter recruiter);

    List<Job> findByCareer(Career career);

//    List<Job> findBySkillsIn(List<Skill> skills);

    List<Job> findByJobIdIn(List<Long> jobIds);

//    Long countByActive(boolean active);

    @Query("SELECT j.company.accountId, COUNT(j) FROM Job j WHERE j.active = true AND j.enabled = true GROUP BY j.company.accountId")
    List<Object[]> countAvailableJobs();
}
