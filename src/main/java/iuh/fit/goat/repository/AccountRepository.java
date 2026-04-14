package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long>,
        JpaSpecificationExecutor<Account> {

    Optional<Account> findByEmailAndDeletedAtIsNull(String email);

    Optional<Account> findByAccountIdAndDeletedAtIsNullAndLockedIsFalse(long accountId);

    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.role WHERE a.email = :email AND a.deletedAt IS NULL")
    Optional<Account> findByEmailWithRole(@Param("email") String email);

    List<Account> findAllByAccountIdInAndDeletedAtIsNull(List<Long> accountIds);

    @Modifying(clearAutomatically = true)
    @Query(
            value =
                    """
                    INSERT INTO account_followed_company (account_id, company_id)
                    SELECT :accountId, c.account_id
                    FROM companies c
                    WHERE c.account_id IN (:companyIds)
                    ON CONFLICT DO NOTHING
                    """,
            nativeQuery = true
    )
    int followCompanies(
            @Param("accountId") Long accountId,
            @Param("companyIds") List<Long> companyIds
    );

    @Modifying(clearAutomatically = true)
    @Query(
            value =
                    """
                    DELETE FROM account_followed_company
                    WHERE account_id = :accountId
                    AND company_id IN (:companyIds)
                    """,
            nativeQuery = true
    )
    int unfollowCompanies(
            @Param("accountId") Long accountId,
            @Param("companyIds") List<Long> companyIds
    );
}
