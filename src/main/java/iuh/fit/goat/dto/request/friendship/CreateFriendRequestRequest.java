package iuh.fit.goat.dto.request.friendship;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateFriendRequestRequest {
    @NotNull(message = "Target user ID cannot be null")
    private Long targetUserId;
}
