package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByConversationIdAndDeletedAtIsNull(Long conversationId);

    Optional<Conversation> findByConversationIdAndAccount_AccountIdAndDeletedAtIsNull(
            Long conversationId,
            Long accountId
    );

    @Query(
            value = """
                SELECT c FROM Conversation c
                WHERE c.account.accountId = :accountId
                AND c.deletedAt IS NULL
                ORDER BY c.pinned DESC, COALESCE(c.updatedAt, c.createdAt) DESC, c.conversationId DESC
                """,
            countQuery = """
                SELECT COUNT(c) FROM Conversation c
                WHERE c.account.accountId = :accountId
                AND c.deletedAt IS NULL
                """
        )
    Page<Conversation> findPageActiveByAccountIdOrderByPinnedAndUpdated(
            @Param("accountId") Long accountId,
            Pageable pageable
    );
}