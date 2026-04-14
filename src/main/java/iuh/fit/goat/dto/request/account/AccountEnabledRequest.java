package iuh.fit.goat.dto.request.account;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AccountEnabledRequest {
    @NotEmpty(message = "accountIds must not be empty")
    private List<@NotNull(message = "accountId must not be null") Long> accountIds;
}
