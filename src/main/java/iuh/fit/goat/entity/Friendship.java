package iuh.fit.goat.entity;


import iuh.fit.goat.enumeration.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.FilterDef;

import java.time.Instant;

@Entity
@Table(
        name = "friendships",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"sender_id", "receiver_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FilterDef(name = "activeFriendshipFilter")
public class Friendship extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long requestId;
    @Enumerated(EnumType.STRING)
    private Status status;
    private Instant respondedAt;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;
}
