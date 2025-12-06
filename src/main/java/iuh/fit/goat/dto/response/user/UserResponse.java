package iuh.fit.goat.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.enumeration.Gender;
import iuh.fit.goat.entity.embeddable.Contact;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private long userId;
    private String address;
    private Contact contact;
    private String username;
    private String fullName;
    private String avatar;
    private Gender gender;
    private LocalDate dob;
    private boolean enabled;
    private RoleUser role;
    private Instant createdAt;
    private Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleUser {
        private long roleId;
        private String name;
    }
}

