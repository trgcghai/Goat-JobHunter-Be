package iuh.fit.goat.dto.response.poll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOptionResponse {
    private String optionId;
    private String text;
    private String createdBy;
    private Instant createdAt;
    private Integer voteCount;
    private Boolean accountVoted;
}

