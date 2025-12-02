package iuh.fit.goat.dto.request.role;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleCreateRequest {
    @NotBlank(message = "Role name is not empty")
    private String name;
    private String description;
}
