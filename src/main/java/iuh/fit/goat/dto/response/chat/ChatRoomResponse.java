package iuh.fit.goat.dto.response.chat;

import iuh.fit.goat.enumeration.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    private Long roomId;
    private ChatRoomType type;
    private String name;
    private String avatar;
    private Integer memberCount;
    private String lastMessagePreview;
    private LocalDateTime lastMessageTime;
}