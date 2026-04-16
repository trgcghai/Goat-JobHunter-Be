package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
        name = "conversation",
        indexes = {
                @Index(name = "idx_conversation_account_id", columnList = "account_id"),
                @Index(name = "idx_conversation_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"account", "messages"})
@FilterDef(name = "activeConversationFilter")
public class Conversation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "conversation_gen")
    @SequenceGenerator(name = "conversation_gen", sequenceName = "conversations_seq", allocationSize = 50)
    private Long conversationId;

    private String title;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned = false;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @OneToMany(mappedBy = "conversation", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeAIMessageFilter",
            condition = "deleted_at IS NULL"
    )
    private List<AIMessage> messages = new ArrayList<>();
}