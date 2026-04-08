package iuh.fit.goat.dto.response.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForwardMessageResponse {
    private Long sourceChatRoomId;
    private String sourceMessageId;
    private Integer totalTargets;
    private Integer successCount;
    private Integer failedCount;
    private List<ForwardMessageSuccessResponse> successes;
    private List<ForwardMessageFailureResponse> failures;
}
