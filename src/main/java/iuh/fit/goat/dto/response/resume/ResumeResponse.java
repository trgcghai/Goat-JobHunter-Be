package iuh.fit.goat.dto.response.resume;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResumeResponse {
    private long resumeId;
    private String title;
    private String fileUrl;
    private String fileName;
    private long fileSize;
    private boolean isDefault;
    private boolean isPublic;
    private Double aiScore;
    private String aiAnalysis;
    private String aiSuggestions;
    private Instant analyzedAt;

    private ResumeApplicant applicant;

    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeApplicant {
        private long accountId;
        private String email;
    }
}
