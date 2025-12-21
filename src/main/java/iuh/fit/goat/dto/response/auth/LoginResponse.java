package iuh.fit.goat.dto.response.auth;

import iuh.fit.goat.entity.Address;
import iuh.fit.goat.enumeration.Gender;
import iuh.fit.goat.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}
