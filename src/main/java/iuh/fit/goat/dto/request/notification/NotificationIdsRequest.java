package iuh.fit.goat.dto.request.notification;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationIdsRequest {
    @NotEmpty(message = "notificationIds must not be empty")
    private List<@NotNull(message = "notificationId must not be null") Long> notificationIds;
}
