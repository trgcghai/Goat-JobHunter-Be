package iuh.fit.goat.dto.request.user;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserEnabledRequest {
    @NotEmpty(message = "userIds must not be empty")
    private List<@NotNull(message = "userId must not be null") Long> userIds;
}
