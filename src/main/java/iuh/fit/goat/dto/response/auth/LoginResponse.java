package iuh.fit.goat.dto.response.auth;

import iuh.fit.goat.common.Gender;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.entity.embeddable.Contact;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private UserLogin user;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserLogin {
        private long userId;
        private Contact contact;
        private LocalDate dob;
        private Gender gender;
        private String fullName;
        private String username;
        private String avatar;
        private String type;
        private boolean enabled;
        private Role role;
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
