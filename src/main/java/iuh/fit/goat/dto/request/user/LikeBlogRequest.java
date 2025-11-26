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
    @NotNull(message = "liked blog must not be null")
    private Blog blog;
    private boolean liked;
}
