package iuh.fit.goat.dto.response.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.enumeration.Status;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class ApplicationResponse {
    private Long applicationId;
    private String email;
    private String coverLetter;
    private Status status;

    private ApplicationJob job;
    private ApplicationApplicant applicant;
    private ApplicationResume resume;
    private ApplicationInterview interview;

    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationJob {
        private Long jobId;
        private String title;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationApplicant {
        private Long accountId;
        private String email;
        private String fullName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationResume {
        private Long resumeId;
        private String fileUrl;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationInterview {
        private Long interviewId;
        private Instant scheduledAt;
    }
}
