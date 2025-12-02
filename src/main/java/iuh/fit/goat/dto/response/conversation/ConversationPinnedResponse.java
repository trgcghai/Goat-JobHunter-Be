package iuh.fit.goat.dto.response.conversation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationPinnedResponse {
    private Long conversationId;
    private boolean pinned;
}
