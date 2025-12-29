package iuh.fit.goat.dto.result.award;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyAwardResult {
    Long companyId;
    Double average;
    Long totalReviews;
}
