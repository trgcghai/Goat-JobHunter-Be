package iuh.fit.goat.dto.response.account;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountLockedResponse {
    private Long accountId;
    private boolean locked;
}
