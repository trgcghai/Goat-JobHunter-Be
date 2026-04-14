package iuh.fit.goat.component.realtime.friendship;

import iuh.fit.goat.dto.response.friendship.FriendshipRealtimeEventResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Component
public class FriendshipRealtimeEventListener {
    private static final String FRIENDSHIP_DESTINATION = "/queue/friendships";

    private final SimpMessagingTemplate messagingTemplate;

    public FriendshipRealtimeEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendshipRealtimeEvent(FriendshipRealtimeEvent event) {
        FriendshipRealtimeEventResponse payload = new FriendshipRealtimeEventResponse(
                event.getType(),
                event.getActorUser(),
                event.getTargetUser(),
                event.getRequestId(),
                event.getRelationshipState(),
                Instant.now()
        );

        this.sendToUser(event.getActorPrincipal(), payload);
        if (event.getTargetPrincipal() != null && !event.getTargetPrincipal().equals(event.getActorPrincipal())) {
            this.sendToUser(event.getTargetPrincipal(), payload);
        }
    }

    private void sendToUser(String userPrincipal, FriendshipRealtimeEventResponse payload) {
        if (userPrincipal == null || userPrincipal.isBlank()) {
            return;
        }

        this.messagingTemplate.convertAndSendToUser(
                userPrincipal,
                FRIENDSHIP_DESTINATION,
                payload
        );
    }
}
