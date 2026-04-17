package iuh.fit.goat.dto.response.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponse {
    private long deviceId;
    private String name;
    private Instant createdAt;
    private AccountDevice account;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountDevice {
        private long accountId;
        private String email;
    }
}
