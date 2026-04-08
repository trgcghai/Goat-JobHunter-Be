package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.profile.UserProfileUpdatedEventResponse;
import iuh.fit.goat.service.ProfileRealtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileRealtimeServiceImpl implements ProfileRealtimeService {
    private static final String USER_PROFILE_UPDATED = "USER_PROFILE_UPDATED";
    private static final String USER_PROFILE_UPDATES_DESTINATION = "/queue/profile-updates";

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void emitUserProfileUpdated(String userPrincipal, String profileType, Object profileData) {
        UserProfileUpdatedEventResponse<Object> eventPayload = new UserProfileUpdatedEventResponse<>(
                USER_PROFILE_UPDATED,
                profileType,
                Instant.now(),
                profileData
        );

        this.messagingTemplate.convertAndSendToUser(
                userPrincipal,
                USER_PROFILE_UPDATES_DESTINATION,
                eventPayload
        );

        log.info("Emitted {} event to user {}", USER_PROFILE_UPDATED, userPrincipal);
    }
}
