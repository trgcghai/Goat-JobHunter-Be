package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.review.ReviewResponse;
import iuh.fit.goat.entity.Review;
import iuh.fit.goat.repository.ReviewRepository;
import iuh.fit.goat.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;

    @Override
    public ResultPaginationResponse handleGetAllReviews(Specification<Review> specification, Pageable pageable) {
        Page<Review> page = this.reviewRepository.findAll(specification, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<ReviewResponse> responses = page.getContent().stream()
                .map(this::handleConvertToReviewResponse)
                .toList();

        return new ResultPaginationResponse(meta, responses);
    }

    @Override
    public Map<Long, Long> handleCountReviewByCompany() {
        return this.reviewRepository.countReviews()
                .stream().collect(
                        Collectors.toMap(
                                row -> (Long) row[0],
                                row -> (Long) row[1]
                        )
                );
    }

    @Override
    public Map<Long, Double> handleAverageRatingByCompany() {
        return this.reviewRepository.averageRatingsByCompany()
                .stream().collect(
                        Collectors.toMap(
                                row -> (Long) row[0],
                                row -> Math.round((Double) row[1] * 10.0) / 10.0
                        )
                );
    }

    @Override
    public List<ReviewResponse> handleGetLatest5Reviews() {
        return this.reviewRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(this::handleConvertToReviewResponse)
                .toList();
    }

    @Override
    public ReviewResponse handleConvertToReviewResponse(Review review) {
        ReviewResponse response = new ReviewResponse();

        response.setReviewId(review.getReviewId());
        response.setRating(review.getRating());
        response.setSummary(review.getSummary());
        response.setExperience(review.getExperience());
        response.setSuggestion(review.getSuggestion());
        response.setRecommended(review.isRecommended());
        response.setVerified(review.isVerified());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());

        if(review.getUser() != null) {
            ReviewResponse.ReviewUser user = new ReviewResponse.ReviewUser(
                    review.getUser().getAccountId(),
                    review.getUser().getEmail()
            );
            response.setUser(user);
        }

        if(review.getCompany() != null) {
            ReviewResponse.ReviewCompany company = new ReviewResponse.ReviewCompany(
                    review.getCompany().getAccountId(),
                    review.getCompany().getName(),
                    review.getCompany().getLogo()
            );
            response.setCompany(company);
        }

        return response;
    }
}
