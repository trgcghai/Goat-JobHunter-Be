package iuh.fit.goat.dto.response.interview;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewStatusResponse {
    private Long interviewId;
    private String status;
}
