package iuh.fit.goat.dto.response.account;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.enumeration.Visibility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountResponse {
    private long accountId;
    private String username;
    private String fullName;
    private String email;
    private String avatar;
    private boolean enabled;
    private boolean locked;
    private Visibility visibility;
    private String phone;
    private RoleAccount role;
    private Instant createdAt;
    private Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleAccount {
        private long roleId;
        private String name;
    }
}
