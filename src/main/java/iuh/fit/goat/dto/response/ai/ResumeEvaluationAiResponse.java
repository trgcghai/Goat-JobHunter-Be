package iuh.fit.goat.dto.response.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeEvaluationAiResponse {
    private Double score;
    private String strengths;
    private String weaknesses;
    private String missingSkills;
    private String skills;
    private String suggestions;
}
