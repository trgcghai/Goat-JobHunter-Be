package iuh.fit.goat.dto.response.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageDeletedEventResponse {
    private String eventType;
    private String chatRoomId;
    private String messageId;
    private String deleteType;
    private Long deletedByAccountId;
    private Instant deletedAt;
}
