package iuh.fit.goat.dto.response.account;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountEnabledResponse {
    private Long accountId;
    private boolean enabled;
}