package iuh.fit.goat.entity;

import iuh.fit.goat.enumeration.ReactionType;
import jakarta.persistence.*;
import lombok.*;

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
@ToString
public class CommentReaction extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long reactionId;
    @Enumerated(EnumType.STRING)
    private ReactionType type;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "comment_id")
    private Comment comment;
}
