package iuh.fit.goat.entity;

import iuh.fit.goat.enumeration.RelationshipState;
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
        name = "user_relationships",
        uniqueConstraints = {
                @UniqueConstraint(name = "uc_user_relationship_pair", columnNames = {"pair_low_id", "pair_high_id"})
        },
        indexes = {
                @Index(name = "idx_url_state", columnList = "relationship_state")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"pairLowUser", "pairHighUser", "blockedBy"})
@FilterDef(name = "activeUserRelationshipFilter")
public class UserRelationship extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_relationship_gen")
    @SequenceGenerator(name = "user_relationship_gen", sequenceName = "user_relationships_seq", allocationSize = 50)
    private long relationshipId;

        @ManyToOne(fetch = LAZY)
        @JoinColumn(name = "pair_low_id", nullable = false)
        private User pairLowUser;

        @ManyToOne(fetch = LAZY)
        @JoinColumn(name = "pair_high_id", nullable = false)
        private User pairHighUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_state", nullable = false)
    private RelationshipState relationshipState;

    private Instant friendsSince;

    private Instant blockedSince;

        @ManyToOne(fetch = LAZY)
        @JoinColumn(name = "blocked_by_id")
        private User blockedBy;
}
