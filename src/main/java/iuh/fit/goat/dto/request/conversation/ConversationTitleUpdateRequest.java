package iuh.fit.goat.dto.request.conversation;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationTitleUpdateRequest {
    @NotBlank(message = "Title must not be blank")
    private String title;
}