package iuh.fit.goat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import iuh.fit.goat.common.Education;
import iuh.fit.goat.common.Level;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantResponse extends UserResponse{
    private boolean availableStatus;
    private Education education;
    private Level level;
}
