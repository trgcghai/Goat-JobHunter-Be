package iuh.fit.goat.dto.request.blog;

import iuh.fit.goat.common.BlogActionType;
import iuh.fit.goat.util.annotation.RequireReasonIfAdmin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RequireReasonIfAdmin
public class BlogIdsRequest {
    @NotEmpty(message = "blogIds must not be empty")
    private List<@NotNull(message = "blogId must not be null") Long> blogIds;
    private String reason;
    @NotNull(message = "mode must not be null")
    private BlogActionType mode;
}
