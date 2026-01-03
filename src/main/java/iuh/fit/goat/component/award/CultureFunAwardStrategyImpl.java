package iuh.fit.goat.component.award;

import iuh.fit.goat.dto.result.award.CompanyAwardResult;
import iuh.fit.goat.enumeration.RatingType;
import iuh.fit.goat.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CultureFunAwardStrategyImpl implements AwardStrategy {
    private final ReviewRepository reviewRepository;

    @Override
    public RatingType getType() {
        return RatingType.CULTURE;
    }

    @Override
    public List<CompanyAwardResult> calculate(int year) {
        return this.reviewRepository.findBestCultureFunCompany(year);
    }
}
