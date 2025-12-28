package iuh.fit.goat.entity.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rating {
    @Min(1) @Max(5)
    @NotNull(message = "Overall rating is required")
    private Integer overall;
    @Min(1) @Max(5)
    @NotNull(message = "Salary & Benefit rating is required")
    private Integer salaryBenefits;
    @Min(1) @Max(5)
    @NotNull(message = "Training & Learning rating is required")
    private Integer trainingLearning;
    @Min(1) @Max(5)
    @NotNull(message = "Management & Care about me rating is required")
    private Integer managementCaresAboutMe;
    @Min(1) @Max(5)
    @NotNull(message = "Culture & Fun rating is required")
    private Integer cultureFun;
    @Min(1) @Max(5)
    @NotNull(message = "Office & Workspace rating is required")
    private Integer officeWorkspace;
}
