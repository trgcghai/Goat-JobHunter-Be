package iuh.fit.goat.dto.request.blog;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnreactBlogRequest {
    @NotEmpty(message = "Blog IDs list cannot be empty")
    private List<Long> blogIds;
}
