package iuh.fit.goat.entity;

import iuh.fit.goat.enumeration.AIMessageRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.FilterDef;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
        name = "ai_message",
        indexes = {
                @Index(name = "idx_ai_message_account_id", columnList = "account_id"),
                @Index(name = "idx_ai_message_conversation_id", columnList = "conversation_id"),
                @Index(name = "idx_ai_message_created_at", columnList = "created_at"),
                @Index(name = "idx_ai_message_conversation_created_at", columnList = "conversation_id, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"conversation", "account"})
@FilterDef(name = "activeAIMessageFilter")
public class AIMessage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ai_message_gen")
    @SequenceGenerator(name = "ai_message_gen", sequenceName = "ai_messages_seq", allocationSize = 50)
    private Long aiMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AIMessageRole role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}