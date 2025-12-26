package iuh.fit.goat.dto.request.blog;

import iuh.fit.goat.enumeration.ReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReactionBlogRequest {
    @NotNull(message = "Blog ID cannot be null")
    private Long blogId;

    @NotNull(message = "Reaction type cannot be null")
    private ReactionType reactionType;
}
