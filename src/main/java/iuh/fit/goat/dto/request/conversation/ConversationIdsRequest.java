package iuh.fit.goat.dto.request.conversation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationIdsRequest {
    @NotEmpty(message = "conversationIds must not be empty")
    private List<@NotNull(message = "conversationId must not be null") Long> conversationIds;
}
