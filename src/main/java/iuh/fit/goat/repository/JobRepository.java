package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Career;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    List<Job> findByRecruiter(Recruiter recruiter);
    List<Job> findByCareer(Career career);
    List<Job> findBySkillsIn(List<Skill> skills);
    List<Job> findByJobIdIn(List<Long> jobIds);

    @Query("SELECT j.recruiter.userId, COUNT(j) FROM Job j GROUP BY j.recruiter.userId")
    List<Object[]> countJobs();
}
