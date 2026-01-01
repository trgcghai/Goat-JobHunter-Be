package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.blog.BlogIdsRequest;
import iuh.fit.goat.dto.request.review.CreateReviewRequest;
import iuh.fit.goat.dto.request.review.ReviewIdsRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.blog.BlogStatusResponse;
import iuh.fit.goat.dto.response.review.RatingResponse;
import iuh.fit.goat.dto.response.review.ReviewResponse;
import iuh.fit.goat.dto.response.review.ReviewStatusResponse;
import iuh.fit.goat.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

public interface ReviewService {
    Review handleCreateReview(CreateReviewRequest review);

    ResultPaginationResponse handleGetAllReviews(Specification<Review> specification, Pageable pageable);

    Map<Long, Long> handleCountReviewByCompany();

    Map<Long, Double> handleOverallAverageRatingByCompany();

    List<ReviewResponse> handleGetLatest5Reviews();

    Long handleCountAllReviews();

    RatingResponse handleGetRatingByCompany(Long companyId);

    Double handleCalculateRecommendedPercentageByCompany(Long companyId);

    Review findByUserAndCompany(Long userId, Long companyId);

    List<ReviewStatusResponse> handleVerifyReviews(ReviewIdsRequest request);

    List<ReviewStatusResponse> handleUnverifyReviews(ReviewIdsRequest request);

    ReviewResponse handleConvertToReviewResponse(Review review);
}
