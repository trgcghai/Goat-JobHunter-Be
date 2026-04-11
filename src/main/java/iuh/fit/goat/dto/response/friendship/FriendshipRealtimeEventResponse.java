package iuh.fit.goat.dto.response.friendship;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.enumeration.FriendshipRealtimeEventType;
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
public class FriendshipRealtimeEventResponse {
    private FriendshipRealtimeEventType type;
    private Long actorUserId;
    private Long targetUserId;
    private Long requestId;
    private RelationshipState relationshipState;
    private Instant emittedAt;
}
