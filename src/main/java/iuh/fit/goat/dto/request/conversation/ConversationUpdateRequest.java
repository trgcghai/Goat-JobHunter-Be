package iuh.fit.goat.dto.request.conversation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationUpdateRequest {
    @NotNull(message = "Conversation ID is required")
    private Long conversationId;
    private String title;
}
