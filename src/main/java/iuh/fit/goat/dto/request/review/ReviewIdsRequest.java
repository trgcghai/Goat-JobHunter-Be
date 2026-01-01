package iuh.fit.goat.dto.request.review;

import iuh.fit.goat.common.ActionType;
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
public class ReviewIdsRequest {
    @NotEmpty(message = "reviewIds must not be empty")
    private List<@NotNull(message = "reviewId must not be null") Long> reviewIds;
    private String reason;
    @NotNull(message = "mode must not be null")
    private ActionType mode;
}
