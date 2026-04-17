package iuh.fit.goat.dto.response.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceNotificationResponse {
    private String message;
    private String deviceName;
    private Instant time;
}
