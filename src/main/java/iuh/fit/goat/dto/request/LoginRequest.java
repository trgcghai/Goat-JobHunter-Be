package iuh.fit.goat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "email is not empty")
    private String email;
    @NotBlank(message = "password is not empty")
    private String password;
}
