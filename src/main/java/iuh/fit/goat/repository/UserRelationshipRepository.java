package iuh.fit.goat.repository;

import iuh.fit.goat.entity.UserRelationship;
import iuh.fit.goat.enumeration.RelationshipState;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserRelationshipRepository extends JpaRepository<UserRelationship, Long> {
        record PairIds(Long pairLowId, Long pairHighId) {
                public static PairIds of(Long firstUserId, Long secondUserId) {
                        if (firstUserId == null || secondUserId == null) {
                                throw new IllegalArgumentException("User IDs cannot be null");
                        }
                        return firstUserId <= secondUserId
                                        ? new PairIds(firstUserId, secondUserId)
                                        : new PairIds(secondUserId, firstUserId);
                }
        }

        Optional<UserRelationship> findByPairLowUser_AccountIdAndPairHighUser_AccountIdAndDeletedAtIsNull(Long pairLowId, Long pairHighId);

        @Query(value = """
                        SELECT pg_advisory_xact_lock(
                                hashtextextended(CONCAT(CAST(:pairLowId AS text), ':', CAST(:pairHighId AS text)), 0)
                        )
                        """, nativeQuery = true)
        Object lockPairForTransactionByCanonicalIds(@Param("pairLowId") Long pairLowId, @Param("pairHighId") Long pairHighId);

    @EntityGraph(attributePaths = {"pairLowUser", "pairHighUser", "blockedBy"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ur FROM UserRelationship ur
                        WHERE ur.pairLowUser.accountId = :pairLowId
                            AND ur.pairHighUser.accountId = :pairHighId
              AND ur.deletedAt IS NULL
            """)
    Optional<UserRelationship> findByPairForUpdate(@Param("pairLowId") Long pairLowId, @Param("pairHighId") Long pairHighId);

    boolean existsByPairLowUser_AccountIdAndPairHighUser_AccountIdAndRelationshipStateAndDeletedAtIsNull(
            Long pairLowId,
            Long pairHighId,
            RelationshipState relationshipState
    );

    @EntityGraph(attributePaths = {"pairLowUser", "pairHighUser"})
    @Query("""
            SELECT ur FROM UserRelationship ur
            WHERE ur.deletedAt IS NULL
              AND ur.relationshipState = :relationshipState
              AND (ur.pairLowUser.accountId = :accountId OR ur.pairHighUser.accountId = :accountId)
              AND (
                    :searchTerm IS NULL
                    OR :searchTerm = ''
                    OR LOWER(
                        COALESCE(
                            CASE
                                WHEN ur.pairLowUser.accountId = :accountId THEN ur.pairHighUser.username
                                ELSE ur.pairLowUser.username
                            END,
                            ''
                        )
                    ) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                    OR LOWER(
                        COALESCE(
                            CASE
                                WHEN ur.pairLowUser.accountId = :accountId THEN ur.pairHighUser.fullName
                                ELSE ur.pairLowUser.fullName
                            END,
                            ''
                        )
                    ) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                    OR LOWER(
                        COALESCE(
                            CASE
                                WHEN ur.pairLowUser.accountId = :accountId THEN ur.pairHighUser.email
                                ELSE ur.pairLowUser.email
                            END,
                            ''
                        )
                    ) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                    OR (
                        :regexPattern IS NOT NULL
                        AND :regexPattern <> ''
                        AND (
                            function(
                                'regexp_match',
                                COALESCE(
                                    CASE
                                        WHEN ur.pairLowUser.accountId = :accountId THEN ur.pairHighUser.username
                                        ELSE ur.pairLowUser.username
                                    END,
                                    ''
                                ),
                                CONCAT('(?i)', :regexPattern)
                            ) IS NOT NULL
                            OR function(
                                'regexp_match',
                                COALESCE(
                                    CASE
                                        WHEN ur.pairLowUser.accountId = :accountId THEN ur.pairHighUser.fullName
                                        ELSE ur.pairLowUser.fullName
                                    END,
                                    ''
                                ),
                                CONCAT('(?i)', :regexPattern)
                            ) IS NOT NULL
                            OR function(
                                'regexp_match',
                                COALESCE(
                                    CASE
                                        WHEN ur.pairLowUser.accountId = :accountId THEN ur.pairHighUser.email
                                        ELSE ur.pairLowUser.email
                                    END,
                                    ''
                                ),
                                CONCAT('(?i)', :regexPattern)
                            ) IS NOT NULL
                        )
                    )
              )
            """)
    Page<UserRelationship> findFriendsByAccountId(
            @Param("accountId") Long accountId,
            @Param("relationshipState") RelationshipState relationshipState,
            @Param("searchTerm") String searchTerm,
            @Param("regexPattern") String regexPattern,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"pairLowUser", "pairHighUser", "blockedBy"})
    @Query("""
            SELECT ur FROM UserRelationship ur
            WHERE ur.deletedAt IS NULL
              AND ur.relationshipState = :relationshipState
              AND ur.blockedBy.accountId = :blockedById
            """)
    Page<UserRelationship> findBlockedByAccountId(
            @Param("blockedById") Long blockedById,
            @Param("relationshipState") RelationshipState relationshipState,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO user_relationships (
                relationship_id,
                created_at,
                created_by,
                updated_at,
                updated_by,
                deleted_at,
                deleted_by,
                pair_low_id,
                pair_high_id,
                relationship_state,
                friends_since,
                blocked_since,
                blocked_by_id
            )
            VALUES (
                nextval('user_relationships_seq'),
                NOW(),
                :actor,
                NOW(),
                :actor,
                NULL,
                NULL,
                :pairLowId,
                :pairHighId,
                :state,
                :friendsSince,
                NULL,
                NULL
            )
            ON CONFLICT (pair_low_id, pair_high_id)
            DO UPDATE SET
                relationship_state = EXCLUDED.relationship_state,
                friends_since = EXCLUDED.friends_since,
                blocked_since = NULL,
                blocked_by_id = NULL,
                deleted_at = NULL,
                deleted_by = NULL,
                updated_at = NOW(),
                updated_by = :actor
            """, nativeQuery = true)
    int upsertFriendRelationship(
            @Param("pairLowId") Long pairLowId,
            @Param("pairHighId") Long pairHighId,
            @Param("state") String relationshipState,
            @Param("friendsSince") Instant friendsSince,
            @Param("actor") String actor
    );

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO user_relationships (
                relationship_id,
                created_at,
                created_by,
                updated_at,
                updated_by,
                deleted_at,
                deleted_by,
                pair_low_id,
                pair_high_id,
                relationship_state,
                friends_since,
                blocked_since,
                blocked_by_id
            )
            VALUES (
                nextval('user_relationships_seq'),
                NOW(),
                :actor,
                NOW(),
                :actor,
                NULL,
                NULL,
                :pairLowId,
                :pairHighId,
                :state,
                NULL,
                :blockedSince,
                :blockedById
            )
            ON CONFLICT (pair_low_id, pair_high_id)
            DO UPDATE SET
                relationship_state = EXCLUDED.relationship_state,
                friends_since = NULL,
                blocked_since = EXCLUDED.blocked_since,
                blocked_by_id = EXCLUDED.blocked_by_id,
                deleted_at = NULL,
                deleted_by = NULL,
                updated_at = NOW(),
                updated_by = :actor
            """, nativeQuery = true)
    int upsertBlockedRelationshipByCanonicalIds(
            @Param("pairLowId") Long pairLowId,
            @Param("pairHighId") Long pairHighId,
            @Param("state") String relationshipState,
            @Param("blockedSince") Instant blockedSince,
            @Param("blockedById") Long blockedById,
            @Param("actor") String actor
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            DELETE FROM UserRelationship ur
            WHERE ur.pairLowUser.accountId = :pairLowId
              AND ur.pairHighUser.accountId = :pairHighId
              AND ur.relationshipState = :relationshipState
              AND ur.deletedAt IS NULL
            """)
    int hardDeleteByCanonicalPairAndState(
            @Param("pairLowId") Long pairLowId,
            @Param("pairHighId") Long pairHighId,
            @Param("relationshipState") RelationshipState relationshipState
    );
}
