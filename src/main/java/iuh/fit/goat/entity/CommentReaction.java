package iuh.fit.goat.entity;

import iuh.fit.goat.enumeration.ReactionType;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
        name = "comment_reactions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"comment_id", "user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"account", "comment"})
public class CommentReaction extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long reactionId;
    @Enumerated(EnumType.STRING)
    private ReactionType type;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;
}
