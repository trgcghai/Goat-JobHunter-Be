package iuh.fit.goat.dto.response.poll;

import iuh.fit.goat.common.MessageEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollVotedEventResponse {
    private MessageEvent eventType;
    private String pollId;
    private String chatRoomId;
    private String accountId;
    private List<String> optionIds;
    private Integer totalVotes;
    private Instant votedAt;
}

