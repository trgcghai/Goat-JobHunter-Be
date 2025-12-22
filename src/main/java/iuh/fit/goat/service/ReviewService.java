package iuh.fit.goat.service;

import java.util.Map;

public interface ReviewService {
    Map<Long, Long> handleCountReviewByCompany();

    Map<Long, Double> handleAverageRatingByCompany();
}
