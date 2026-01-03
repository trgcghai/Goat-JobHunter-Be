package iuh.fit.goat.dto.request.review;

import iuh.fit.goat.entity.embeddable.Rating;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {
    @NotNull(message = "Rating is required")
    @Valid
    private Rating rating;
    @NotBlank(message = "Summary is required")
    private String summary;
    @NotBlank(message = "Experience is required")
    private String experience;
    @NotBlank(message = "Suggestion is required")
    private String suggestion;
    private boolean recommended;
    @NotNull(message = "Company id is required")
    private Long companyId;
}
