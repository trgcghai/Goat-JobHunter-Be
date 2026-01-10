package iuh.fit.goat.dto.response.interview;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.enumeration.InterviewStatus;
import iuh.fit.goat.enumeration.InterviewType;
import iuh.fit.goat.enumeration.Status;
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
public class InterviewResponse {
    private long interviewId;
    private Instant scheduledAt;
    private Integer durationMinutes;
    private InterviewType type;
    private InterviewStatus status;
    private String location;
    private String meetingLink;
    private String notes;
    private String feedback;
    private Integer rating;

    private InterviewUser interviewer;
    private InterviewApplication application;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewUser{
        private long accountId;
        private String fullName;
        private String email;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewApplication{
        private long applicationId;
        private String email;
        private Status status;
        private String fullName;
    }

}
