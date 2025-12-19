package iuh.fit.goat.dto.response.recruiter;

import iuh.fit.goat.dto.response.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterResponse extends UserResponse {
    private String position;
    private CompanySummary company;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanySummary {
        private long companyId;
        private String name;
    }
}