package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long>, JpaSpecificationExecutor<Message> {
    Long countByConversation_ConversationId(Long conversationId);

    Page<Message> findByConversation_ConversationId(Long conversationId, Pageable pageable);
}
