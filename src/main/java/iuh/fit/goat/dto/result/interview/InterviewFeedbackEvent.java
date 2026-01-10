package iuh.fit.goat.dto.result.interview;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewFeedbackEvent {
    private String applicantEmail;
    private String companyEmail;
    private String applicantName;
    private String companyName;
    private String feedback;
    private int rating;
}
