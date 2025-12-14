package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iuh.fit.goat.enumeration.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"actors", "recipient", "comment"})
@FilterDef(name = "activeNotificationFilter")
public class Notification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long notificationId;
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    private boolean seen = false;

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
    @Filter(
            name = "activeAccountFilter",
            condition = "deleted_at IS NULL"
    )
    private List<User> actors = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    public User getLastActor() {
        if (actors != null && !actors.isEmpty()) {
            return actors.getLast();
        }
        return null;
    }

    public int getActorCount() {
        return actors != null ? actors.size() : 0;
    }
}
