package iuh.fit.goat.entity.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rating {
    @Min(1) @Max(5)
    private Integer overall;
    @Min(1) @Max(5)
    private Integer salaryBenefits;
    @Min(1) @Max(5)
    private Integer trainingLearning;
    @Min(1) @Max(5)
    private Integer managementCaresAboutMe;
    @Min(1) @Max(5)
    private Integer cultureFun;
    @Min(1) @Max(5)
    private Integer officeWorkspace;
}
