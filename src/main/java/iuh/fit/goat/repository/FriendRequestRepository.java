package iuh.fit.goat.repository;

import iuh.fit.goat.entity.FriendRequest;
import iuh.fit.goat.enumeration.FriendRequestStatus;
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
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
        boolean existsByPairLowUser_AccountIdAndPairHighUser_AccountIdAndStatusAndDeletedAtIsNull(
            Long pairLowId,
            Long pairHighId,
            FriendRequestStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fr FROM FriendRequest fr WHERE fr.requestId = :requestId AND fr.deletedAt IS NULL")
    Optional<FriendRequest> findActiveByIdForUpdate(@Param("requestId") Long requestId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE FriendRequest fr
            SET fr.status = :newStatus,
                fr.respondedAt = :respondedAt
                                                WHERE fr.pairLowUser.accountId = :pairLowId
                                                        AND fr.pairHighUser.accountId = :pairHighId
              AND fr.status = :currentStatus
              AND fr.requestId <> :excludedRequestId
              AND fr.deletedAt IS NULL
            """)
    int updateStatusForOtherRequests(
            @Param("pairLowId") Long pairLowId,
            @Param("pairHighId") Long pairHighId,
            @Param("currentStatus") FriendRequestStatus currentStatus,
            @Param("newStatus") FriendRequestStatus newStatus,
            @Param("respondedAt") Instant respondedAt,
            @Param("excludedRequestId") Long excludedRequestId
    );
}
