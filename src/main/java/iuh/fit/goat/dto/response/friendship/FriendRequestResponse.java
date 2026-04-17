package iuh.fit.goat.dto.response.friendship;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.enumeration.FriendRequestStatus;
import iuh.fit.goat.enumeration.RelationshipState;
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
public class FriendRequestResponse {
    private Long requestId;
    private Long senderId;
    private Long receiverId;
    private FriendRequestStatus status;
    private RelationshipState relationshipState;
    private Instant requestedAt;
    private Instant respondedAt;
}
