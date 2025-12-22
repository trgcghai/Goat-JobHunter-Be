package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.review.ReviewResponse;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.entity.Review;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.CompanyService;
import iuh.fit.goat.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    private final ReviewService reviewService;
    private final CompanyService companyService;

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

    @GetMapping("/companies/count")
    public ResponseEntity<Map<Long, Long>> countReviewByCompany() {
        Map<Long, Long> result = this.reviewService.handleCountReviewByCompany();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/companies/ratings/average")
    public ResponseEntity<Map<Long, Double>> averageRatingByCompany() {
        Map<Long, Double> result = this.reviewService.handleAverageRatingByCompany();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countAllReviews() {
        return ResponseEntity.ok(this.reviewService.handleCountAllReviews());
    }

}
