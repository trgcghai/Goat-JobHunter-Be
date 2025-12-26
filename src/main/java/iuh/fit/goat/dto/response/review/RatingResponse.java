package iuh.fit.goat.dto.response.review;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {
    private Map<String, RatingStats> ratings;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingStats {
        private Double average;
        private Map<Integer, Integer> distribution;
    }
}
