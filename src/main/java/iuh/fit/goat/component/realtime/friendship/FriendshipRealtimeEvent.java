package iuh.fit.goat.component.realtime.friendship;

import iuh.fit.goat.enumeration.FriendshipRealtimeEventType;
import iuh.fit.goat.enumeration.RelationshipState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FriendshipRealtimeEvent {
    private final FriendshipRealtimeEventType type;
    private final Long actorUserId;
    private final Long targetUserId;
    private final Long requestId;
    private final RelationshipState relationshipState;
    private final String actorPrincipal;
    private final String targetPrincipal;
}
