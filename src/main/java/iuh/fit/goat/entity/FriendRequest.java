package iuh.fit.goat.entity;

import iuh.fit.goat.enumeration.FriendRequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.FilterDef;

import java.time.Instant;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
        name = "friend_requests",
        indexes = {
                @Index(name = "idx_frq_sender_status", columnList = "sender_id, status"),
                @Index(name = "idx_frq_receiver_status", columnList = "receiver_id, status"),
                @Index(name = "idx_frq_pair", columnList = "pair_low_id, pair_high_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"sender", "receiver", "pairLowUser", "pairHighUser"})
@FilterDef(name = "activeFriendRequestFilter")
public class FriendRequest extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "friend_request_gen")
    @SequenceGenerator(name = "friend_request_gen", sequenceName = "friend_requests_seq", allocationSize = 50)
    private long requestId;

        @ManyToOne(fetch = LAZY)
        @JoinColumn(name = "pair_low_id", nullable = false)
        private User pairLowUser;

        @ManyToOne(fetch = LAZY)
        @JoinColumn(name = "pair_high_id", nullable = false)
        private User pairHighUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendRequestStatus status;

    @Column(nullable = false)
    private Instant requestedAt;

    private Instant respondedAt;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;
}
