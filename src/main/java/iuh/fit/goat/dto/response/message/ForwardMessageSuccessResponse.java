package iuh.fit.goat.dto.response.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForwardMessageSuccessResponse {
    private Long targetChatRoomId;
    private MessageResponse message;
}
