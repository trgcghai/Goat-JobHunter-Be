package iuh.fit.goat.repository;

import iuh.fit.goat.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    User findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByAccountIdIn(List<Long> accountIds);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.email = :email")
    Optional<User> findByEmailWithRole(@Param("email") String email);

    @Modifying(clearAutomatically = true)
    @Query(
        value =
            """
            INSERT INTO user_followed_company (user_id, company_id)
            SELECT :userId, c.account_id
            FROM companies c
            WHERE c.account_id IN (:companyIds)
            ON CONFLICT DO NOTHING
            """,
        nativeQuery = true
    )
    int followCompanies(
            @Param("userId") Long userId,
            @Param("companyIds") List<Long> companyIds
    );

    @Modifying(clearAutomatically = true)
    @Query(
        value =
        """
        DELETE FROM user_followed_company
        WHERE user_id = :userId
        AND company_id IN (:companyIds)
        """,
        nativeQuery = true
    )
    int unfollowCompanies(
            @Param("userId") Long userId,
            @Param("companyIds") List<Long> companyIds
    );

    @Query("""
        SELECT u FROM User u
        WHERE u.deletedAt IS NULL
        AND u.role.name <> 'SUPER_ADMIN'
        AND u.email <> :currentUserEmail
        AND (
            :searchTerm IS NULL
            OR :searchTerm = ''
            OR u.email = :searchTerm
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
        """)
    Page<User> searchUsers(
        @Param("searchTerm") String searchTerm,
        @Param("currentUserEmail") String currentUserEmail,
        Pageable pageable
    );
}
