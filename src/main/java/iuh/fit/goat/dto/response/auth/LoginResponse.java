package iuh.fit.goat.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.entity.Address;
import iuh.fit.goat.enumeration.CompanySize;
import iuh.fit.goat.enumeration.Gender;
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
    private RoleAccount role;
    private UserCompany company;

    private String name;
    private String description;
    private String logo;
    private String coverPhoto;
    private String website;
    private CompanySize size;
    private boolean verified;
    private String country;
    private String industry;
    private String workingDays;
    private String overtimePolicy;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class UserCompany {
        private long accountId;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleAccount {
        private long roleId;
        private String name;
    }
}
