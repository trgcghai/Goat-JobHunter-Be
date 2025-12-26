package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    List<Review> findTop5ByVerifiedIsTrueOrderByCreatedAtDesc();

    Long countByVerifiedIsTrue();

    @Query("SELECT r.company.accountId, COUNT(r) FROM Review r WHERE r.verified = true GROUP BY r.company.accountId")
    List<Object[]> countReviews();

    @Query("SELECT r.company.accountId, AVG(r.rating.overall) FROM Review r WHERE r.verified = true GROUP BY r.company.accountId")
    List<Object[]> averageOverallRatingsByCompany();

    @Query(
        """
        SELECT
            CASE
                WHEN :ratingType = 'overall' THEN AVG(r.rating.overall)
                WHEN :ratingType = 'salary' THEN AVG(r.rating.salaryBenefits)
                WHEN :ratingType = 'training' THEN AVG(r.rating.trainingLearning)
                WHEN :ratingType = 'management' THEN AVG(r.rating.managementCaresAboutMe)
                WHEN :ratingType = 'culture' THEN AVG(r.rating.cultureFun)
                WHEN :ratingType = 'office' THEN AVG(r.rating.officeWorkspace)
            END
        FROM Review r
        WHERE r.company.accountId = :companyId
        AND r.verified = true
        """
    )
    Double averageRating(@Param("companyId") Long companyId, @Param("ratingType") String ratingType);

    @Query(
        """
        SELECT
            CASE
                WHEN :ratingType = 'overall' THEN r.rating.overall
                WHEN :ratingType = 'salary' THEN r.rating.salaryBenefits
                WHEN :ratingType = 'training' THEN r.rating.trainingLearning
                WHEN :ratingType = 'management' THEN r.rating.managementCaresAboutMe
                WHEN :ratingType = 'culture' THEN r.rating.cultureFun
                WHEN :ratingType = 'office' THEN r.rating.officeWorkspace
            END AS star,
            COUNT(r)
        FROM Review r
        WHERE r.company.accountId = :companyId
        AND r.verified = true
        GROUP BY star
        """
    )
    List<Object[]> countDistributionByRating( @Param("companyId") Long companyId, @Param("ratingType") String ratingType);

}
