package iuh.fit.goat.dto.request;

import iuh.fit.goat.entity.Recruiter;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FollowRecruiterRequest {
    @NotNull(message = "user is not empty")
    private Long userId;
    @NotNull(message = "Followed recruiters must not be null")
    private List<Recruiter> followedRecruiters;
}
