package iuh.fit.goat.dto.request.interview;

import iuh.fit.goat.enumeration.InterviewType;
import iuh.fit.goat.util.annotation.RequireMeetingLinkIfTypeOnline;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RequireMeetingLinkIfTypeOnline
public class CreateInterviewRequest {
    @NotNull(message = "Scheduled time is required")
    @Future(message = "Scheduled time must be in the future")
    private Instant scheduledAt;

    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Interview duration must be at least 15 minutes")
    private Integer durationMinutes;

    @NotNull(message = "Interview type is required")
    private InterviewType type;

    @NotBlank(message = "Location is required")
    private String location;

    @Pattern(
            regexp = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$",
            message = "Meeting link must be a valid URL"
    )
    private String meetingLink;

    @Size(max = 200, message = "Notes must not exceed 200 characters")
    private String notes;

    @Positive(message = "Interviewer ID must be positive")
    private long interviewerId;

    @NotEmpty(message = "applicationIds is required")
    private List<Long> applicationIds;
}
