package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Application;
import iuh.fit.goat.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {
    List<Application> findByApplicant(Applicant applicant);
    List<Application> findByJob(Job job);

    @Query(
        """
        SELECT COUNT(a) FROM Application a
        WHERE a.applicant.email = :email AND a.job.jobId = :jobId
        """
    )
    Long countApplicationsByApplicantAndJob(String email, Long jobId);
}
