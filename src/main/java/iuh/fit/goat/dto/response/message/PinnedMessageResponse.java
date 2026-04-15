package iuh.fit.goat.dto.response.message;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PinnedMessageResponse {
    private String chatRoomId;
    private String messageId;
    private String pinnedBy;
    private Instant pinnedAt;
    private MessageResponse message;
}

