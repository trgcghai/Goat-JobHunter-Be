package iuh.fit.goat.dto.request.chat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupChatRequest {
    @NotEmpty(message = "At least two members are required")
    @Size(min = 2, message = "Group chat requires at least 2 members besides owner")
    private List<Long> memberAccountIds;
}