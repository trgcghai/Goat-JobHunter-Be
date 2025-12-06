package iuh.fit.goat.dto.response.applicant;

import iuh.fit.goat.dto.response.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import iuh.fit.goat.enumeration.Education;
import iuh.fit.goat.enumeration.Level;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantResponse extends UserResponse {
    private boolean availableStatus;
    private Education education;
    private Level level;
}
