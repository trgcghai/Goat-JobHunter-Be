package iuh.fit.goat.dto.request.message;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForwardMessageRequest {

    @NotNull(message = "Target chat room IDs are required")
    @NotEmpty(message = "At least one target chat room ID is required")
    private List<Long> targetChatRoomIds;
}
