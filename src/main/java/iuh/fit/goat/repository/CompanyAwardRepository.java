package iuh.fit.goat.repository;

import iuh.fit.goat.entity.CompanyAward;
import iuh.fit.goat.enumeration.RatingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyAwardRepository extends JpaRepository<CompanyAward, Long> {
    boolean existsByCompany_AccountIdAndTypeAndYear(Long companyId, RatingType type, int year);
}
