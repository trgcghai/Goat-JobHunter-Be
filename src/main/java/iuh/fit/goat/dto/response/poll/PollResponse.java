package iuh.fit.goat.dto.response.poll;

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
public class PollResponse {
    private String pollId;
    private Long chatRoomId;
    private String messageId;
    private String createdBy;
    private String question;
    private List<PollOptionResponse> options;
    private Boolean multipleChoice;
    private Boolean allowAddOption;
    private Boolean pinned;
    private Boolean isClosed;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
}

