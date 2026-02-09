package iuh.fit.goat.dto.request.chat;

import iuh.fit.goat.enumeration.ChatRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberRoleRequest {
    @NotNull(message = "Role is required")
    private ChatRole role;
}