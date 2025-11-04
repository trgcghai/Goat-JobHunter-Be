package iuh.fit.goat.dto.request;

import iuh.fit.goat.entity.Job;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SaveJobRequest {
    @NotNull(message = "user is not empty")
    private Long userId;
    @NotNull(message = "Saved jobs must not be null")
    private List<Job> savedJobs;
}
