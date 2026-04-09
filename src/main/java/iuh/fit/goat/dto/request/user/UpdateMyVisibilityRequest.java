package iuh.fit.goat.dto.request.user;

import iuh.fit.goat.enumeration.Visibility;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMyVisibilityRequest {
    @NotNull(message = "visibility must not be null")
    private Visibility visibility;
}
