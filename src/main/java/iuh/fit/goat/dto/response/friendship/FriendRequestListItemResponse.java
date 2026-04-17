package iuh.fit.goat.dto.response.friendship;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.enumeration.FriendRequestStatus;
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
public class FriendRequestListItemResponse {
    private Long requestId;
    private FriendRequestStatus status;
    private Instant requestedAt;
    private Instant respondedAt;
    private Direction direction;
    private FriendUserSnippetResponse counterpart;

    public enum Direction {
        RECEIVED,
        SENT
    }
}
