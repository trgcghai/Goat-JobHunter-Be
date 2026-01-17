package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>,
        JpaSpecificationExecutor<Company> {

    Optional<Company> findByEmail(String email);

    Optional<Company> findByAccountIdAndDeletedAtIsNull(Long id);

    Optional<Company> findByNameIgnoreCaseAndDeletedAtIsNull(String name);

    List<Company> findByAccountIdIn(List<Long> accountIds);

    @Query("SELECT c FROM Company c LEFT JOIN FETCH c.role WHERE c.email = :email")
    Optional<Company> findByEmailWithRole(@Param("email") String email);

    @Query(
        """
        SELECT DISTINCT s.skillId, s.name
        FROM Company c
        JOIN c.jobs j
        JOIN j.skills s
        WHERE c.accountId = :companyId
        """
    )
    List<Object[]> findDistinctSkillsByCompanyId(@Param("companyId") Long companyId);

    @Query(
        """
        SELECT c.name
        FROM Company c
        WHERE c.enabled = true AND c.deletedAt IS NULL
        """
    )
    List<String> getAllCompanyNames();

    @Modifying
    @Query("UPDATE Address a SET a.deletedAt = CURRENT_TIMESTAMP WHERE a.account.accountId = :companyId")
    void softDeleteAddresses(@Param("companyId") long companyId);

    @Modifying
    @Query("UPDATE Notification n SET n.deletedAt = CURRENT_TIMESTAMP WHERE n.recipient.accountId = :companyId")
    void softDeleteRecipientNotifications(@Param("companyId") long companyId);

    @Modifying
    @Query("UPDATE Job j SET j.deletedAt = CURRENT_TIMESTAMP WHERE j.company.accountId = :companyId")
    void softDeleteJobs(@Param("companyId") long companyId);

    @Modifying
    @Query("UPDATE Recruiter r SET r.deletedAt = CURRENT_TIMESTAMP WHERE r.company.accountId = :companyId")
    void softDeleteRecruiters(@Param("companyId") long companyId);

    @Modifying
    @Query("UPDATE Review r SET r.deletedAt = CURRENT_TIMESTAMP WHERE r.company.accountId = :companyId")
    void softDeleteReviews(@Param("companyId") long companyId);

    @Modifying
    @Query("UPDATE CompanyAward c SET c.deletedAt = CURRENT_TIMESTAMP WHERE c.company.accountId = :companyId")
    void softDeleteAwards(@Param("companyId") long companyId);

}
