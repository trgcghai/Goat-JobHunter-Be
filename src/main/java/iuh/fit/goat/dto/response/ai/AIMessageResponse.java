package iuh.fit.goat.dto.response.ai;

import iuh.fit.goat.enumeration.AIMessageRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AIMessageResponse {
    private Long aiMessageId;
    private AIMessageRole role;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
}