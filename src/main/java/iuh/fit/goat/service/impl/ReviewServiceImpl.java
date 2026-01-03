package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.ActionType;
import iuh.fit.goat.dto.request.review.ReviewIdsRequest;
import iuh.fit.goat.dto.response.review.ReviewStatusResponse;
import iuh.fit.goat.enumeration.RatingType;
import iuh.fit.goat.dto.request.review.CreateReviewRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.review.RatingResponse;
import iuh.fit.goat.dto.response.review.ReviewResponse;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.entity.Review;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.repository.CompanyRepository;
import iuh.fit.goat.repository.ReviewRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.EmailNotificationService;
import iuh.fit.goat.service.ReviewService;
import iuh.fit.goat.util.RatingDistributionUtil;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final EmailNotificationService emailNotificationService;

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Override
    public Review handleCreateReview(CreateReviewRequest review) {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        User currentUser = this.userRepository.findByEmail(email);

        Review newReview = new Review();
        newReview.setRating(review.getRating());
        newReview.setSummary(review.getSummary());
        newReview.setExperience(review.getExperience());
        newReview.setSuggestion(review.getSuggestion());
        newReview.setRecommended(review.isRecommended());
        newReview.setUser(currentUser);

        if(review.getCompanyId() != null) {
            Optional<Company> company = this.companyRepository.findById(review.getCompanyId());
            company.ifPresent(newReview::setCompany);
        }

        return this.reviewRepository.save(newReview);
    }

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
    public Map<Long, Double> handleOverallAverageRatingByCompany() {
        return this.reviewRepository.averageOverallRatingsByCompany()
                .stream().collect(
                        Collectors.toMap(
                                row -> (Long) row[0],
                                row -> Math.round((Double) row[1] * 10.0) / 10.0
                        )
                );
    }

    @Override
    public List<ReviewResponse> handleGetLatest5Reviews() {
        return this.reviewRepository.findTop5ByVerifiedIsTrueAndEnabledIsTrueAndDeletedAtNullOrderByCreatedAtDesc()
                .stream()
                .map(this::handleConvertToReviewResponse)
                .toList();
    }

    @Override
    public Long handleCountAllReviews() {
        return this.reviewRepository.countByVerifiedIsTrueAndEnabledIsTrueAndDeletedAtNull();
    }

    @Override
    public RatingResponse handleGetRatingByCompany(Long companyId) {
        Map<String, RatingResponse.RatingStats> result = new LinkedHashMap<>();

        for (RatingType type : RatingType.values()) {

            Double average = this.reviewRepository.averageRating(companyId, type.getType());
            Map<Integer, Integer> distribution = RatingDistributionUtil.build(
                    this.reviewRepository.countDistributionByRating(companyId, type.getType())
            );

            RatingResponse.RatingStats stats = new RatingResponse.RatingStats(average, distribution);

            result.put(type.getValue(), stats);
        }

        return new RatingResponse(result);
    }

    @Override
    public Double handleCalculateRecommendedPercentageByCompany(Long companyId) {
        return this.reviewRepository.calculateRecommendedPercentageByCompany(companyId);
    }

    @Override
    public Review findByUserAndCompany(Long userId, Long companyId) {
        return this.reviewRepository.findByUser_AccountIdAndCompany_AccountId(userId, companyId);
    }

    @Override
    public List<ReviewStatusResponse> handleVerifyReviews(ReviewIdsRequest request) {
        List<Review> reviews = this.reviewRepository.findAllById(request.getReviewIds());
        if (reviews.isEmpty()) return Collections.emptyList();

        reviews.forEach(review -> review.setVerified(true));
        this.reviewRepository.saveAll(reviews);

        Map<String, List<Review>> reviewByEmail =
                reviews.stream().collect(Collectors.groupingBy(review -> review.getUser().getEmail()));

        reviewByEmail.forEach((email, rs) -> {
            if (rs.isEmpty()) return;

            this.emailNotificationService.handleSendReviewActionNotice(
                    email, rs.getFirst().getUser().getUsername(),
                    rs, null, ActionType.ACCEPT
            );
        });

        return reviews.stream().map(
                review -> new ReviewStatusResponse(
                        review.getReviewId(),
                        review.isVerified()
                )
        ).collect(Collectors.toList());
    }

    @Override
    public List<ReviewStatusResponse> handleUnverifyReviews(ReviewIdsRequest request) {
        List<Review> reviews = this.reviewRepository.findAllById(request.getReviewIds());
        if (reviews.isEmpty()) return Collections.emptyList();

        reviews.forEach(review -> review.setVerified(false));
        this.reviewRepository.saveAll(reviews);

        Map<String, List<Review>> reviewByEmail =
                reviews.stream().collect(Collectors.groupingBy(review -> review.getUser().getEmail()));

        reviewByEmail.forEach((email, rs) -> {
            if (rs.isEmpty()) return;

            this.emailNotificationService.handleSendReviewActionNotice(
                    email, rs.getFirst().getUser().getUsername(),
                    rs, request.getReason(), ActionType.REJECT
            );
        });

        return reviews.stream().map(
                review -> new ReviewStatusResponse(
                        review.getReviewId(),
                        review.isVerified()
                )
        ).collect(Collectors.toList());
    }

    @Override
    public List<ReviewStatusResponse> handleEnableReviews(ReviewIdsRequest request) {
        List<Review> reviews = this.reviewRepository.findAllById(request.getReviewIds());
        if (reviews.isEmpty()) return Collections.emptyList();

        reviews.forEach(review -> review.setEnabled(true));
        this.reviewRepository.saveAll(reviews);

        Map<String, List<Review>> reviewByEmail =
                reviews.stream().collect(Collectors.groupingBy(review -> review.getUser().getEmail()));

        reviewByEmail.forEach((email, rs) -> {
            if (rs.isEmpty()) return;

            this.emailNotificationService.handleSendReviewActionNotice(
                    email, rs.getFirst().getUser().getUsername(),
                    rs, null, ActionType.ENABLE
            );
        });

        return reviews.stream().map(
                review -> new ReviewStatusResponse(
                        review.getReviewId(),
                        review.isEnabled()
                )
        ).collect(Collectors.toList());
    }

    @Override
    public List<ReviewStatusResponse> handleDisableReviews(ReviewIdsRequest request) {
        List<Review> reviews = this.reviewRepository.findAllById(request.getReviewIds());
        if (reviews.isEmpty()) return Collections.emptyList();

        reviews.forEach(review -> review.setEnabled(false));
        this.reviewRepository.saveAll(reviews);

        Map<String, List<Review>> reviewByEmail =
                reviews.stream().collect(Collectors.groupingBy(review -> review.getUser().getEmail()));

        reviewByEmail.forEach((email, rs) -> {
            if (rs.isEmpty()) return;

            this.emailNotificationService.handleSendReviewActionNotice(
                    email, rs.getFirst().getUser().getUsername(),
                    rs, request.getReason(), ActionType.DISABLE
            );
        });

        return reviews.stream().map(
                review -> new ReviewStatusResponse(
                        review.getReviewId(),
                        review.isVerified()
                )
        ).collect(Collectors.toList());
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
        response.setEnabled(review.isEnabled());
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
