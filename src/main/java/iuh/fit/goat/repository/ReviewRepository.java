package iuh.fit.goat.repository;

import iuh.fit.goat.dto.result.award.CompanyAwardResult;
import iuh.fit.goat.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    List<Review> findTop5ByVerifiedIsTrueAndEnabledIsTrueAndDeletedAtNullOrderByCreatedAtDesc();

    Long countByVerifiedIsTrueAndEnabledIsTrueAndDeletedAtNull();

    Review findByUser_AccountIdAndCompany_AccountId(Long userId, Long companyId);

    @Query("SELECT r.company.accountId, COUNT(r) FROM Review r WHERE r.verified = true AND r.enabled = true AND r.deletedAt IS NULL GROUP BY r.company.accountId")
    List<Object[]> countReviews();

    @Query("SELECT r.company.accountId, AVG(r.rating.overall) FROM Review r WHERE r.verified = true AND r.enabled = true AND r.deletedAt IS NULL GROUP BY r.company.accountId")
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
        AND r.enabled = true
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
        AND r.enabled = true
        AND r.deletedAt IS NULL
        GROUP BY star
        """
    )
    List<Object[]> countDistributionByRating( @Param("companyId") Long companyId, @Param("ratingType") String ratingType);

    @Query(
        """
        SELECT
            CASE
                WHEN COUNT(r) = 0 THEN 0
                ELSE (SUM(CASE WHEN r.recommended = true THEN 1 ELSE 0 END) * 100.0 / COUNT(r))
            END
        FROM Review r
        WHERE r.company.accountId = :companyId
        AND r.verified = true
        AND r.enabled = true
        AND r.deletedAt IS NULL
        """
    )
    Double calculateRecommendedPercentageByCompany(@Param("companyId") Long companyId);

    @Query(
            """
            SELECT new iuh.fit.goat.dto.result.award.CompanyAwardResult
            (
                   r.company.accountId,
                   AVG(r.rating.salaryBenefits),
                   COUNT(r)
            )
            FROM Review r
            WHERE r.verified = true
            AND r.enabled = true
            AND r.deletedAt IS NULL
            AND YEAR(r.createdAt) = :year
            GROUP BY r.company.accountId
            HAVING COUNT(r) >= 20 AND AVG(r.rating.overall) = 5.0
            ORDER BY AVG(r.rating.overall) DESC, COUNT(r) DESC
            """
    )
    List<CompanyAwardResult> findBestOverallCompany(@Param("year") int year);

    @Query(
            """
            SELECT new iuh.fit.goat.dto.result.award.CompanyAwardResult
            (
                   r.company.accountId,
                   AVG(r.rating.salaryBenefits),
                   COUNT(r)
            )
            FROM Review r
            WHERE r.verified = true
            AND r.enabled = true
            AND r.deletedAt IS NULL
            AND YEAR(r.createdAt) = :year
            GROUP BY r.company.accountId
            HAVING COUNT(r) >= 20 AND AVG(r.rating.salaryBenefits) >= 4.0
            ORDER BY AVG(r.rating.salaryBenefits) DESC, COUNT(r) DESC
            """
    )
    List<CompanyAwardResult> findBestSalaryBenefitsCompany(@Param("year") int year);

    @Query(
            """
            SELECT new iuh.fit.goat.dto.result.award.CompanyAwardResult
            (
                   r.company.accountId,
                   AVG(r.rating.salaryBenefits),
                   COUNT(r)
            )
            FROM Review r
            WHERE r.verified = true
            AND r.enabled = true
            AND r.deletedAt IS NULL
            AND YEAR(r.createdAt) = :year
            GROUP BY r.company.accountId
            HAVING COUNT(r) >= 20 AND AVG(r.rating.trainingLearning) >= 4.0
            ORDER BY AVG(r.rating.trainingLearning) DESC, COUNT(r) DESC
            """
    )
    List<CompanyAwardResult> findBestTrainingLearningCompany(@Param("year") int year);

    @Query(
            """
            SELECT new iuh.fit.goat.dto.result.award.CompanyAwardResult
            (
                   r.company.accountId,
                   AVG(r.rating.salaryBenefits),
                   COUNT(r)
            )
            FROM Review r
            WHERE r.verified = true
            AND r.enabled = true
            AND r.deletedAt IS NULL
            AND YEAR(r.createdAt) = :year
            GROUP BY r.company.accountId
            HAVING COUNT(r) >= 20 AND AVG(r.rating.managementCaresAboutMe) >= 4.0
            ORDER BY AVG(r.rating.managementCaresAboutMe) DESC, COUNT(r) DESC
            """
    )
    List<CompanyAwardResult> findBestManagementCaresAboutMeCompany(@Param("year") int year);

    @Query(
        """
        SELECT new iuh.fit.goat.dto.result.award.CompanyAwardResult
        (
               r.company.accountId,
               AVG(r.rating.salaryBenefits),
               COUNT(r)
        )
        FROM Review r
        WHERE r.verified = true
        AND r.enabled = true
        AND r.deletedAt IS NULL
        AND YEAR(r.createdAt) = :year
        GROUP BY r.company.accountId
        HAVING COUNT(r) >= 20 AND AVG(r.rating.cultureFun) >= 4.0
        ORDER BY AVG(r.rating.cultureFun) DESC, COUNT(r) DESC
        """
    )
    List<CompanyAwardResult> findBestCultureFunCompany(@Param("year") int year);

    @Query(
            """
            SELECT new iuh.fit.goat.dto.result.award.CompanyAwardResult
            (
                   r.company.accountId,
                   AVG(r.rating.salaryBenefits),
                   COUNT(r)
            )
            FROM Review r
            WHERE r.verified = true
            AND r.enabled = true
            AND r.deletedAt IS NULL
            AND YEAR(r.createdAt) = :year
            GROUP BY r.company.accountId
            HAVING COUNT(r) >= 20 AND AVG(r.rating.officeWorkspace) >= 4.0
            ORDER BY AVG(r.rating.officeWorkspace) DESC, COUNT(r) DESC
            """
    )
    List<CompanyAwardResult> findBestOfficeWorkspaceCompany(@Param("year") int year);

}
