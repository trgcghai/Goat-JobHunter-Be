package iuh.fit.goat.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePasswordRequest {
    @NotBlank(message = "current password is not empty")
    private String currentPassword;
    @NotBlank(message = "new password is not empty")
    private String newPassword;
    @NotBlank(message = " re-password is not empty")
    private String rePassword;
}
