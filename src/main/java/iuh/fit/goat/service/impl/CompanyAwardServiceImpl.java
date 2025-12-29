package iuh.fit.goat.service.impl;

import iuh.fit.goat.component.award.AwardStrategy;
import iuh.fit.goat.component.award.AwardStrategyFactory;
import iuh.fit.goat.dto.result.award.CompanyAwardResult;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.entity.CompanyAward;
import iuh.fit.goat.enumeration.RatingType;
import iuh.fit.goat.repository.CompanyAwardRepository;
import iuh.fit.goat.repository.CompanyRepository;
import iuh.fit.goat.service.CompanyAwardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyAwardServiceImpl implements CompanyAwardService {
    private final AwardStrategyFactory awardStrategyFactory;
    private final CompanyAwardRepository companyAwardRepository;
    private final CompanyRepository companyRepository;

    @Override
    public void calculateAwardsForYear(int year) {
        for (RatingType type : RatingType.values()) {

            AwardStrategy strategy = this.awardStrategyFactory.getStrategy(type);
            List<CompanyAwardResult> result = strategy.calculate(year);

            if (result == null || result.isEmpty()) continue;

            List<Long> companyIds = result.stream().map(CompanyAwardResult::getCompanyId).toList();
            List<Company> companies = this.companyRepository.findByAccountIdIn(companyIds);
            for(CompanyAwardResult awardResult : result) {
                Company company = companies.stream()
                        .filter(c -> c.getAccountId() == awardResult.getCompanyId())
                        .findFirst()
                        .orElse(null);
                if (company == null) continue;

                boolean existed = this.companyAwardRepository.existsByCompany_AccountIdAndTypeAndYear(company.getAccountId(), type, year);
                if (existed) continue;

                CompanyAward award = CompanyAward.builder()
                        .type(type)
                        .year(year)
                        .average(awardResult.getAverage())
                        .totalReviews(awardResult.getTotalReviews())
                        .company(company)
                        .build();

                this.companyAwardRepository.save(award);
            }

        }
    }
}
