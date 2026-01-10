package iuh.fit.goat.dto.request.interview;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewIdsRequest {
    @NotEmpty(message = "interviewIds is required")
    private List<Long> interviewIds;
    private String reason;
}
