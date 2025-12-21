package iuh.fit.goat.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.entity.Address;
import iuh.fit.goat.enumeration.Gender;
import iuh.fit.goat.entity.embeddable.Contact;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private long accountId;
    private String username;
    private String email;
    private String phone;
    private List<Address> addresses;
    private String fullName;
    private String avatar;
    private Gender gender;
    private LocalDate dob;
    private boolean enabled;
    private String coverPhoto;
    private String headline;
    private String bio;
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

