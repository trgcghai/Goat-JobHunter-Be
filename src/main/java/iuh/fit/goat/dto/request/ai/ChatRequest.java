package iuh.fit.goat.dto.request.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatRequest {
    private Long conversationId;
    @NotNull(message = "Message is required")
    @NotBlank(message = "Message is not empty")
    private String message;
}
