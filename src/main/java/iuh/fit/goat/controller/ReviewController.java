package iuh.fit.goat.controller;

import iuh.fit.goat.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    private final ReviewService reviewService;

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

}
