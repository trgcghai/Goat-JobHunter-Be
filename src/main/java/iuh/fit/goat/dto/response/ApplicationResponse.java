package iuh.fit.goat.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import iuh.fit.goat.common.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicationResponse {
    private long applicationId;
    private String email;
    private String resumeUrl;
    private String recruiterName;
    @Enumerated(EnumType.STRING)
    private Status status;

    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    private UserApplication user;
    private JobApplication job;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserApplication {
        private long userId;
        private String fullName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobApplication {
        private long jobId;
        private String title;
    }
}
