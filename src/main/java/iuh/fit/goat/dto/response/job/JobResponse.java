package iuh.fit.goat.dto.response.job;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.entity.Skill;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import iuh.fit.goat.common.Level;
import iuh.fit.goat.common.WorkingType;
import iuh.fit.goat.entity.Career;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobResponse {
    private long jobId;
    private String title;
    private String location;
    private double salary;
    private int quantity;
    private String description;
    private Level level;
    private WorkingType workingType;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private List<Skill> skills;
    private Career career;
    private RecruiterJob recruiter;

    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecruiterJob {
        private long userId;
        private String fullName;
    }
}