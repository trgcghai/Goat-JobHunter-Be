package iuh.fit.goat.dto.response.chat;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnreadMessageResponse {
    private Long chatRoomId;
    private long unreadCount;
}
