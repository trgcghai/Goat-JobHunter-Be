package iuh.fit.goat.entity;

import iuh.fit.goat.common.NotificationType;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long notificationId;
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    private boolean seen;
    private Instant createdAt;
    private String createdBy;

    @ManyToOne
    @JoinColumn(name = "blog_id")
    private Blog blog;

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private User actor;

    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @ManyToOne
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @ManyToOne
    @JoinColumn(name = "reply_id")
    private Comment reply;

    @ManyToOne
    @JoinColumn(name = "replied_on_comment_id")
    private Comment repliedOnComment;

    @PrePersist
    public void handleBeforeCreate(){
        this.createdAt = Instant.now();
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
    }
}
