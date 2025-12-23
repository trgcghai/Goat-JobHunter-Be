package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    @Query("SELECT r.company.accountId, COUNT(r) FROM Review r GROUP BY r.company.accountId")
    List<Object[]> countReviews();

    @Query("SELECT r.company.accountId, AVG(r.rating.overall) FROM Review r GROUP BY r.company.accountId")
    List<Object[]> averageOverallRatingsByCompany();

    List<Review> findTop5ByOrderByCreatedAtDesc();
}
