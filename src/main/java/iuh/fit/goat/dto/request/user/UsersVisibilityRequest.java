package iuh.fit.goat.dto.request.user;

import iuh.fit.goat.enumeration.Visibility;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UsersVisibilityRequest {
    @NotEmpty(message = "accountIds must not be empty")
    private List<@NotNull(message = "accountId must not be null") Long> accountIds;

    @NotNull(message = "visibility must not be null")
    private Visibility visibility;
}
