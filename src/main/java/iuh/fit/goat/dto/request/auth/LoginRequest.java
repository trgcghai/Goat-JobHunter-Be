package iuh.fit.goat.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LoginRequest {
    @NotBlank(message = "email is not empty")
    private String email;
    @NotBlank(message = "password is not empty")
    private String password;
}
