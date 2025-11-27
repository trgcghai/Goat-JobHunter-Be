package iuh.fit.goat.dto.request.user;

import iuh.fit.goat.entity.Blog;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LikeBlogRequest {
    @NotNull(message = "blogId must not be null")
    private Long blogId;
    private boolean liked;
}
