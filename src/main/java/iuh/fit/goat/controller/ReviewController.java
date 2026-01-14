package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.request.review.CreateReviewRequest;
import iuh.fit.goat.dto.request.review.ReviewIdsRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.review.RatingResponse;
import iuh.fit.goat.dto.response.review.ReviewResponse;
import iuh.fit.goat.dto.response.review.ReviewStatusResponse;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.entity.Review;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.CompanyService;
import iuh.fit.goat.service.ReviewService;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    private final ReviewService reviewService;
    private final CompanyService companyService;
    private final UserService userService;

    @PutMapping("/verified")
    public ResponseEntity<List<ReviewStatusResponse>> verifyReviews(
            @Valid @RequestBody ReviewIdsRequest request
    ) {
        List<ReviewStatusResponse> result = this.reviewService.handleVerifyReviews(request);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PutMapping("/unverified")
    public ResponseEntity<List<ReviewStatusResponse>> unverifyReviews(
            @Valid @RequestBody ReviewIdsRequest request
    ) {
        List<ReviewStatusResponse> result = this.reviewService.handleUnverifyReviews(request);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PutMapping("/enabled")
    public ResponseEntity<List<ReviewStatusResponse>> enableReviews(
            @Valid @RequestBody ReviewIdsRequest request
    ) {
        List<ReviewStatusResponse> result = this.reviewService.handleEnableReviews(request);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PutMapping("/disabled")
    public ResponseEntity<List<ReviewStatusResponse>> disableReviews(
            @Valid @RequestBody ReviewIdsRequest request
    ) {
        List<ReviewStatusResponse> result = this.reviewService.handleDisableReviews(request);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest review) throws InvalidException {
        Long companyId = review.getCompanyId();
        Company company = this.companyService.handleGetCompanyById(companyId);
        if(company == null) {
            throw new InvalidException("Company not found");
        }

        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        User user = this.userService.handleGetUserByEmail(email);
        if(user == null) {
            throw new InvalidException("User not found");
        }

        Review existingReview = this.reviewService.findByUserAndCompany(user.getAccountId(), companyId);
        if(existingReview != null) {
            throw new InvalidException("You have already reviewed this company");
        }

        Review newReview = this.reviewService.handleCreateReview(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.reviewService.handleConvertToReviewResponse(newReview));
    }

    @GetMapping
    public ResponseEntity<ResultPaginationResponse> getAllReviews(
            @Filter Specification<Review> specification, Pageable pageable
    ) {
        ResultPaginationResponse response = this.reviewService.handleGetAllReviews(specification, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/companies/{name}")
    public ResponseEntity<ResultPaginationResponse> getAllReviewsByCompany
    (
            @PathVariable("name") String name,
            @Filter Specification<Review> spec, Pageable pageable
    ) throws InvalidException {
        String normalizedName = name.replace("-", " ");
        Company company = this.companyService.handleGetCompanyByName(normalizedName);
        if (company == null) {
            throw new InvalidException("Company not found");
        }

        Specification<Review> baseSpec = spec != null ? spec : Specification.unrestricted();
        Specification<Review> finalSpec = baseSpec
                .and((root, query, cb) -> cb.isTrue(root.get("verified")))
                .and((root, query, cb) -> cb.isTrue(root.get("enabled")))
                .and((root, query, cb) -> cb.isNull(root.get("deletedAt")))
                .and((root, query, cb)
                        -> cb.equal(
                                cb.lower(root.join("company").get("name")),
                                company.getName().toLowerCase()
                        )
                );

        ResultPaginationResponse response = this.reviewService.handleGetAllReviews(finalSpec, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/latest")
    public ResponseEntity<List<ReviewResponse>> getLatest5Reviews() {
        List<ReviewResponse> responses = this.reviewService.handleGetLatest5Reviews();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countAllReviews() {
        return ResponseEntity.ok(this.reviewService.handleCountAllReviews());
    }

    @GetMapping("/companies/count")
    public ResponseEntity<Map<Long, Long>> countReviewByCompany() {
        Map<Long, Long> result = this.reviewService.handleCountReviewByCompany();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/companies/ratings/average")
    public ResponseEntity<Map<Long, Double>> averageOverallRatingByCompany() {
        Map<Long, Double> result = this.reviewService.handleOverallAverageRatingByCompany();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/companies/{companyId}/ratings/summary")
    public ResponseEntity<RatingResponse> getRatingByCompany(@PathVariable("companyId") String companyId)
            throws InvalidException
    {
        if (!SecurityUtil.checkValidNumber(companyId)) throw new InvalidException("Id is number");

        Company company = this.companyService.handleGetCompanyById(Long.parseLong(companyId));
        if (company == null) throw new InvalidException("Company not found");

        RatingResponse response = this.reviewService.handleGetRatingByCompany(Long.parseLong(companyId));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/companies/{companyId}/recommendation-rate")
    public ResponseEntity<Double> calculateRecommendedPercentageByCompany(@PathVariable("companyId") String companyId)
            throws InvalidException
    {
        if (!SecurityUtil.checkValidNumber(companyId)) throw new InvalidException("Id is number");

        Company company = this.companyService.handleGetCompanyById(Long.parseLong(companyId));
        if (company == null) throw new InvalidException("Company not found");

        return ResponseEntity.ok(this.reviewService.handleCalculateRecommendedPercentageByCompany(Long.parseLong(companyId)));
    }

}
