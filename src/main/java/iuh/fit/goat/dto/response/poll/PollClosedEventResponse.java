package iuh.fit.goat.dto.response.poll;

import iuh.fit.goat.common.MessageEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollClosedEventResponse {
    private MessageEvent eventType;
    private String pollId;
    private String chatRoomId;
    private String messageId;
    private Instant closedAt;
}

