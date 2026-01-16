package iuh.fit.goat.repository;

import iuh.fit.goat.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("""
        SELECT DISTINCT cr FROM ChatRoom cr
        JOIN cr.members cm
        WHERE cm.user.accountId = :accountId
        AND cr.deletedAt IS NULL
        AND cm.deletedAt IS NULL
        ORDER BY cr.updatedAt DESC
    """)
    Page<ChatRoom> findChatRoomsByMemberAccountId(
            @Param("accountId") Long accountId,
            Pageable pageable
    );

    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
            "JOIN cr.members m1 " +
            "JOIN cr.members m2 " +
            "WHERE cr.type = 'DIRECT' " +
            "AND m1.user.accountId = :userId1 " +
            "AND m2.user.accountId = :userId2 " +
            "AND m1.deletedAt IS NULL " +
            "AND m2.deletedAt IS NULL")
    Optional<ChatRoom> findDirectChatRoomBetweenUsers(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2
    );
}