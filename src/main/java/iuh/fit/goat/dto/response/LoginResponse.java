package iuh.fit.goat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.entity.Notification;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    @JsonProperty("access_token")
    private String accessToken;
    private UserLogin user;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserLogin {
        private long userId;
        private String email;
        private String fullName;
        private String username;
        private String avatar;
        private String type;
        private boolean enabled;
        private Role role;
        private List<Job> savedJobs;
        private List<Recruiter> followedRecruiters;
        private List<Notification> actorNotifications;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserGetAccount {
        private UserLogin user;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInsideToken {
        private long userId;
        private String email;
        private String fullName;
    }
}
