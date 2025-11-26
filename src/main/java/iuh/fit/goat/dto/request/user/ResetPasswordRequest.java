package iuh.fit.goat.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    @NotBlank(message = "email is not empty")
    private String email;
    @NotBlank(message = "new password is not empty")
    private String newPassword;
}
