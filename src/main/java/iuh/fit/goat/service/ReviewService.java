package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.review.ReviewResponse;
import iuh.fit.goat.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

public interface ReviewService {
    ResultPaginationResponse handleGetAllReviews(Specification<Review> specification, Pageable pageable);

    Map<Long, Long> handleCountReviewByCompany();

    Map<Long, Double> handleAverageRatingByCompany();

    List<ReviewResponse> handleGetLatest5Reviews();

    Long handleCountAllReviews();

    ReviewResponse handleConvertToReviewResponse(Review review);
}
