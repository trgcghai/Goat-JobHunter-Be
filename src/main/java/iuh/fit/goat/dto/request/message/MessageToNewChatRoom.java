package iuh.fit.goat.dto.request.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageToNewChatRoom {
    @NotBlank(message = "Content is not empty")
    private String content;

    @NotNull(message = "AccountId is not null")
    private Long accountId;
}
