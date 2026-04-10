package iuh.fit.goat.repository;

import iuh.fit.goat.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByRoomIdAndDeletedAtIsNull(Long chatRoomId);

    @Query("""
        SELECT DISTINCT cr FROM ChatRoom cr
        JOIN cr.members cm
        WHERE cm.account.accountId = :accountId
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
            "AND m1.account.accountId = :userId1 " +
            "AND m2.account.accountId = :userId2 " +
            "AND m1.deletedAt IS NULL " +
            "AND m2.deletedAt IS NULL")
    Optional<ChatRoom> findDirectChatRoomBetweenUsers(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2
    );

        @Query("""
                SELECT cr FROM ChatRoom cr
                WHERE cr.type = 'DIRECT'
                AND cr.deletedAt IS NULL
                AND EXISTS (
                        SELECT 1 FROM ChatMember cm1
                        WHERE cm1.room = cr
                        AND cm1.account.accountId = :userId1
                        AND cm1.deletedAt IS NULL
                )
                AND EXISTS (
                        SELECT 1 FROM ChatMember cm2
                        WHERE cm2.room = cr
                        AND cm2.account.accountId = :userId2
                        AND cm2.deletedAt IS NULL
                )
                ORDER BY COALESCE(cr.updatedAt, cr.createdAt) DESC, cr.roomId DESC
        """)
        List<ChatRoom> findDirectChatRoomsBetweenUsersOrderByLatest(
                        @Param("userId1") Long userId1,
                        @Param("userId2") Long userId2
        );
}