package iuh.fit.goat.entity;

import iuh.fit.goat.enumeration.ReactionType;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
        name = "blog_reactions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"blog_id", "user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "blog"})
public class BlogReaction extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "blog_reaction_gen")
    @SequenceGenerator(name = "blog_reaction_gen", sequenceName = "blog_reactions_reaction_id_seq", allocationSize = 1)
    private long reactionId;
    @Enumerated(EnumType.STRING)
    private ReactionType type;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "blog_id")
    private Blog blog;
}
