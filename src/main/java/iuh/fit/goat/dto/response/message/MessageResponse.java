package iuh.fit.goat.dto.response.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.enumeration.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {
    private Long messageId;
    private MessageRole role;
    private String content;
    private Instant createdAt;
}
