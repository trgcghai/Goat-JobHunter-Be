package iuh.fit.goat.dto.request.interview;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackInterviewRequest {
    @NotNull(message = "InterviewID is required")
    private Long interviewId;
    @Size(max = 50, message = "Notes must not exceed 50 characters")
    private String feedback;
    @Min(1) @Max(5)
    @NotNull(message = "Rating is required")
    private Integer rating;
}
