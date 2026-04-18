package iuh.fit.goat.repository;

import iuh.fit.goat.entity.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
    List<ChatMember> findByRoomRoomIdAndDeletedAtIsNull(Long roomId);

    @Query(
            "SELECT cm.lastReadMessageSk FROM ChatMember cm " +
            "WHERE cm.room.roomId = :roomId AND cm.account.accountId = :accountId AND cm.deletedAt IS NULL"
    )
    Optional<String> findLastReadMessageSk(@Param("roomId") Long roomId, @Param("accountId") Long accountId);

    @Modifying
    @Transactional
    @Query(
            "UPDATE ChatMember cm " +
            "SET cm.lastReadMessageSk = :messageSk " +
            "WHERE cm.room.roomId = :roomId AND cm.account.accountId = :accountId AND cm.deletedAt IS NULL"
    )
    void updateLastReadMessageId(
            @Param("roomId") Long roomId,
            @Param("accountId") Long accountId,
            @Param("messageSk") String messageSk
    );
}




