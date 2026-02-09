package iuh.fit.goat.repository;

import iuh.fit.goat.entity.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
    List<ChatMember> findByRoomRoomIdAndDeletedAtIsNull(Long roomId);
}
