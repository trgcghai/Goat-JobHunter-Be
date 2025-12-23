package iuh.fit.goat.dto.response.blog;

import iuh.fit.goat.enumeration.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BlogReactionCheckResponse {
    private Long blogId;
    private ReactionType reactionType;
}
