package iuh.fit.goat.dto.response.review;

import iuh.fit.goat.entity.embeddable.Rating;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private long reviewId;
    private Rating rating;
    private String summary;
    private String experience;
    private String suggestion;
    private boolean recommended;
    private boolean verified;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
    private ReviewUser user;
    private ReviewCompany company;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewUser {
        private long accountId;
        private String email;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewCompany {
        private long accountId;
        private String name;
        private String logo;
    }

}
