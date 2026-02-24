package iuh.fit.goat.dto.response.resume;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeEvaluationResponse {
    private Long resumeEvaluationId;
    private Double score;
    private String strengths;
    private String weaknesses;
    private String missingSkills;
    private String skills;
    private String suggestions;
    private String aiModel;

    private Resume resume;

    private Instant createdAt;
    private Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resume {
        private Long resumeId;
    }
}
