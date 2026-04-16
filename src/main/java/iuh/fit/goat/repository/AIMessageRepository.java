package iuh.fit.goat.repository;

import iuh.fit.goat.entity.AIMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIMessageRepository extends JpaRepository<AIMessage, Long> {

    @Query("""
            SELECT m FROM AIMessage m
            WHERE m.conversation.conversationId = :conversationId
            AND m.deletedAt IS NULL
            ORDER BY m.createdAt DESC
        """)
    List<AIMessage> findRecentByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);

    @Query(
            value = """
                SELECT m FROM AIMessage m
                WHERE m.conversation.conversationId = :conversationId
                AND m.deletedAt IS NULL
                ORDER BY COALESCE(m.createdAt, m.updatedAt) DESC, m.aiMessageId DESC
                """,
            countQuery = """
                SELECT COUNT(m) FROM AIMessage m
                WHERE m.conversation.conversationId = :conversationId
                AND m.deletedAt IS NULL
                """
        )
            Page<AIMessage> findPageByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);
}