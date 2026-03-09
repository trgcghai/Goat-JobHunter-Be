package iuh.fit.goat.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.entity.Address;
import iuh.fit.goat.enumeration.Gender;
import iuh.fit.goat.entity.Role;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    private long accountId;
    private String email;
    private String phone;
    private List<Address> addresses;
    private LocalDate dob;
    private Gender gender;
    private String fullName;
    private String username;
    private String avatar;
    private String type;
    private boolean enabled;
    private Role role;
    private UserCompany company;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class UserCompany {
        private long accountId;
        private String name;
    }
}
