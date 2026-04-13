package iuh.fit.goat.component.realtime.friendship;

import iuh.fit.goat.dto.response.friendship.FriendUserSnippetResponse;
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
    private final FriendUserSnippetResponse actorUser;
    private final FriendUserSnippetResponse targetUser;
    private final Long requestId;
    private final RelationshipState relationshipState;
    private final String actorPrincipal;
    private final String targetPrincipal;
}
