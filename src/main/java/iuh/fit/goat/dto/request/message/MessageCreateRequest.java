package iuh.fit.goat.dto.request.message;

import iuh.fit.goat.common.MessageRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageCreateRequest {
    @NotNull(message = "conversation ID is required")
    private Long conversationId;
    @NotNull(message = "Role is required")
    private MessageRole role;
    @NotBlank(message = "message is not empty")
    private String content;
}
