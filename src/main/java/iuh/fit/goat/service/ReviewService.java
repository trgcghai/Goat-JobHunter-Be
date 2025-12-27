package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.review.RatingResponse;
import iuh.fit.goat.dto.response.review.ReviewResponse;
import iuh.fit.goat.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

public interface ReviewService {
    ResultPaginationResponse handleGetAllReviews(Specification<Review> specification, Pageable pageable);

    Map<Long, Long> handleCountReviewByCompany();

    Map<Long, Double> handleOverallAverageRatingByCompany();

    List<ReviewResponse> handleGetLatest5Reviews();

    Long handleCountAllReviews();

    RatingResponse handleGetRatingByCompany(Long companyId);

    Double handleCalculateRecommendedPercentageByCompany(Long companyId);

    ReviewResponse handleConvertToReviewResponse(Review review);
}
