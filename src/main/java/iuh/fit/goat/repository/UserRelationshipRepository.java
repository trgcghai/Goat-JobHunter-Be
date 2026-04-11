package iuh.fit.goat.repository;

import iuh.fit.goat.entity.UserRelationship;
import iuh.fit.goat.enumeration.RelationshipState;
import jakarta.persistence.LockModeType;
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
    Optional<UserRelationship> findByPairLowIdAndPairHighIdAndDeletedAtIsNull(Long pairLowId, Long pairHighId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ur FROM UserRelationship ur
            WHERE ur.pairLowId = :pairLowId
              AND ur.pairHighId = :pairHighId
              AND ur.deletedAt IS NULL
            """)
    Optional<UserRelationship> findByPairForUpdate(@Param("pairLowId") Long pairLowId, @Param("pairHighId") Long pairHighId);

    boolean existsByPairLowIdAndPairHighIdAndRelationshipStateAndDeletedAtIsNull(
            Long pairLowId,
            Long pairHighId,
            RelationshipState relationshipState
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
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
}
