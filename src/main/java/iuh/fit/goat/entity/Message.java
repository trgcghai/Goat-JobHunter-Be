package iuh.fit.goat.entity;

import iuh.fit.goat.enumeration.MessageRole;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long messageId;
    @Enumerated(EnumType.STRING)
    private MessageRole role;
    @NotBlank(message = "Content is not empty")
    @Column(columnDefinition = "TEXT")
    private String content;

    protected Instant createdAt;
    protected String createdBy;

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @PrePersist
    public void handleBeforeCreate(){
        this.createdAt = Instant.now();
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        this.conversation.setUpdatedAt(Instant.now());
        this.conversation.setUpdatedBy(this.createdBy);
    }
}
