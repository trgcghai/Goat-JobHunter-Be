package iuh.fit.goat.repository;

import iuh.fit.goat.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    List<Job> findByCompany(Company company);

    Optional<Job> findByJobIdAndDeletedAtIsNull(long id);

    List<Job> findByCareer(Career career);

    List<Job> findBySkillsIn(List<Skill> skills);

    List<Job> findByJobIdIn(List<Long> jobIds);

    Long countByActive(boolean active);

    @Query(
        """
        SELECT j.company.accountId, COUNT(j)
        FROM Job j
        WHERE j.enabled = true AND j.deletedAt IS NULL
        GROUP BY j.company.accountId
        """
    )
    List<Object[]> countAvailableJobs();

    @Query(
        """
        SELECT DISTINCT j.jobId
        FROM Job j
        WHERE
        (
            (
                j.level IN (
                    SELECT sj.level
                    FROM User u
                    JOIN u.savedJobs sj
                    WHERE u.accountId = :accountId
                )
                OR
                j.level IN (
                    SELECT aj.level
                    FROM Application a
                    JOIN a.job aj
                    WHERE a.applicant.accountId = :accountId
                )
        
                OR
                j.workingType IN (
                    SELECT sj.workingType
                    FROM User u
                    JOIN u.savedJobs sj
                    WHERE u.accountId = :accountId
                )
                OR
                j.workingType IN (
                    SELECT aj.workingType
                    FROM Application a
                    JOIN a.job aj
                    WHERE a.applicant.accountId = :accountId
                )
        
                OR
                j.career.careerId IN (
                    SELECT sj.career.careerId
                    FROM User u
                    JOIN u.savedJobs sj
                    WHERE u.accountId = :accountId
                )
                OR
                j.career.careerId IN (
                    SELECT aj.career.careerId
                    FROM Application a
                    JOIN a.job aj
                    WHERE a.applicant.accountId = :accountId
                )
            )
            OR
            j.company.accountId IN (
                SELECT c.accountId
                FROM User u
                JOIN u.followedCompanies c
                WHERE u.accountId = :accountId
            )
        )
        AND j.enabled = true AND j.deletedAt IS NULL
        """
    )
    List<Long> findRelatedJobsByCurrentUser(@Param("accountId") Long accountId);
}
