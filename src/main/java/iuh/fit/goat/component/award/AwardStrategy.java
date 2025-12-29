package iuh.fit.goat.component.award;

import iuh.fit.goat.dto.result.award.CompanyAwardResult;
import iuh.fit.goat.enumeration.RatingType;

import java.util.List;

public interface AwardStrategy {
    RatingType getType();
    List<CompanyAwardResult> calculate(int year);
}
