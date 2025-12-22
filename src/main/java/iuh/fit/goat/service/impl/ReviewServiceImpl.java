package iuh.fit.goat.service.impl;

import iuh.fit.goat.repository.ReviewRepository;
import iuh.fit.goat.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;

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
}
