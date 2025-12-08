package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iuh.fit.goat.enumeration.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    private boolean seen = false;

    private Instant createdAt;

    @ManyToOne
    @JoinColumn(name = "blog_id")
    private Blog blog;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "notification_actors",
            joinColumns = @JoinColumn(name = "notification_id"),
            inverseJoinColumns = @JoinColumn(name = "actor_id")
    )
    @JsonIgnore
    @ToString.Exclude
    private List<User> actors = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
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
    }

    public User getLastActor() {
        if (actors != null && !actors.isEmpty()) {
            return actors.get(actors.size() - 1);
        }
        return null;
    }

    public int getActorCount() {
        return actors != null ? actors.size() : 0;
    }
}
