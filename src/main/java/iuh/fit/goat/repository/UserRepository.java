package iuh.fit.goat.repository;

import iuh.fit.goat.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    User findByEmail(String email);

    Optional<User> findByAccountIdAndDeletedAtIsNull(Long accountId);

    boolean existsByEmail(String email);

    List<User> findByAccountIdIn(List<Long> accountIds);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.email = :email")
    Optional<User> findByEmailWithRole(@Param("email") String email);

    @Query("""
        SELECT u FROM User u
        WHERE u.deletedAt IS NULL
        AND u.role.name <> 'SUPER_ADMIN'
        AND u.email <> :currentUserEmail
        AND u.enabled = TRUE
        AND u.locked = FALSE
        AND NOT EXISTS (
            SELECT ur.relationshipId FROM UserRelationship ur
            WHERE ur.deletedAt IS NULL
            AND ur.relationshipState = iuh.fit.goat.enumeration.RelationshipState.BLOCKED
            AND (
                (ur.pairLowUser.accountId = :currentUserId AND ur.pairHighUser.accountId = u.accountId)
                OR
                (ur.pairLowUser.accountId = u.accountId AND ur.pairHighUser.accountId = :currentUserId)
            )
        )
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
        @Param("currentUserId") Long currentUserId,
        Pageable pageable
    );
}
