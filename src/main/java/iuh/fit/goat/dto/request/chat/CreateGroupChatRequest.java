package iuh.fit.goat.dto.request.chat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupChatRequest {
    @NotNull(message = "Account IDs are required")
    @NotEmpty(message = "At least 2 members are required")
    @Size(min = 2, message = "Group chat must have at least 2 members")
    private List<Long> accountIds;

    private String name;
    private String avatar;
}