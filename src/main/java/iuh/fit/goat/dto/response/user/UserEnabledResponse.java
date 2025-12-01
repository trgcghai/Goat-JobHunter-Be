package iuh.fit.goat.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserEnabledResponse {
    private Long userId;
    private boolean enabled;
}