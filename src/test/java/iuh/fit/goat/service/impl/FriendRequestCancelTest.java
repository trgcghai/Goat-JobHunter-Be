package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.friendship.FriendRequestResponse;
import iuh.fit.goat.entity.FriendRequest;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.FriendRequestStatus;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.repository.FriendRequestRepository;
import iuh.fit.goat.repository.UserRelationshipRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendRequestCancelTest {

    @Mock private FriendRequestRepository friendRequestRepository;
    @Mock private UserRelationshipRepository userRelationshipRepository;
    @Mock private UserRepository userRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FriendRequestServiceImpl friendRequestService;

    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setRoleId(1L);
        role.setName("USER");

        sender = new User();
        sender.setAccountId(1L);
        sender.setEmail("sender@example.com");
        sender.setUsername("sender");
        sender.setPassword("hash");
        sender.setRole(role);

        receiver = new User();
        receiver.setAccountId(2L);
        receiver.setEmail("receiver@example.com");
        receiver.setUsername("receiver");
        receiver.setPassword("hash");
        receiver.setRole(role);
    }

    @Test
    void handleCancelFriendRequest_shouldCancelBySender() throws Exception {
        FriendRequest request = new FriendRequest();
        request.setRequestId(22L);
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequestStatus.PENDING);

        when(friendRequestRepository.findActiveByIdForUpdate(22L)).thenReturn(Optional.of(request));

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("sender@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("sender@example.com")).thenReturn(Optional.of(sender));
            when(friendRequestRepository.save(any(FriendRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            FriendRequestResponse response = friendRequestService.handleCancelFriendRequest(22L);

            assertThat(response.getStatus()).isEqualTo(FriendRequestStatus.CANCELED);
            verify(friendRequestRepository).save(request);
        }
    }

    @Test
    void handleCancelFriendRequest_invalidId_null() {
        org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                friendRequestService.handleCancelFriendRequest(null)
        );
    }

    @Test
    void handleCancelFriendRequest_invalidId_negative() {
        org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                friendRequestService.handleCancelFriendRequest(-5L)
        );
    }

    @Test
    void handleCancelFriendRequest_notFound_throwsNotFound() {
        when(friendRequestRepository.findActiveByIdForUpdate(33L)).thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("sender@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("sender@example.com")).thenReturn(Optional.of(sender));

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.NotFoundException.class, () ->
                    friendRequestService.handleCancelFriendRequest(33L)
            );
        }
    }

    @Test
    void handleCancelFriendRequest_notPending_throwsConflict() {
        FriendRequest req = new FriendRequest(); req.setRequestId(44L); req.setSender(sender); req.setReceiver(receiver); req.setStatus(iuh.fit.goat.enumeration.FriendRequestStatus.ACCEPTED);
        when(friendRequestRepository.findActiveByIdForUpdate(44L)).thenReturn(Optional.of(req));

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("sender@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("sender@example.com")).thenReturn(Optional.of(sender));

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.ConflictException.class, () ->
                    friendRequestService.handleCancelFriendRequest(44L)
            );
        }
    }

    @Test
    void handleCancelFriendRequest_onlySenderAllowed_throwsInvalid() {
        FriendRequest req = new FriendRequest(); req.setRequestId(55L); req.setSender(sender); req.setReceiver(receiver); req.setStatus(FriendRequestStatus.PENDING);
        when(friendRequestRepository.findActiveByIdForUpdate(55L)).thenReturn(Optional.of(req));

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            // current user is receiver -> cannot cancel
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("receiver@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("receiver@example.com")).thenReturn(Optional.of(receiver));

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                    friendRequestService.handleCancelFriendRequest(55L)
            );
        }
    }

    @Test
    void handleCancelFriendRequest_unauthenticated_throwsInvalid() {
        FriendRequest req = new FriendRequest(); req.setRequestId(66L); req.setSender(sender); req.setReceiver(receiver); req.setStatus(FriendRequestStatus.PENDING);

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.empty());

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                    friendRequestService.handleCancelFriendRequest(66L)
            );
        }
    }

    @Test
    void handleCancelFriendRequest_eventPublished_and_responseFields() throws Exception {
        FriendRequest request = new FriendRequest();
        request.setRequestId(77L);
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequestStatus.PENDING);

        when(friendRequestRepository.findActiveByIdForUpdate(77L)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("sender@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("sender@example.com")).thenReturn(Optional.of(sender));

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

            FriendRequestResponse response = friendRequestService.handleCancelFriendRequest(77L);

            assertThat(response.getRequestId()).isEqualTo(77L);
            assertThat(response.getSenderId()).isEqualTo(1L);
            assertThat(response.getReceiverId()).isEqualTo(2L);
            assertThat(response.getStatus()).isEqualTo(FriendRequestStatus.CANCELED);
            assertThat(response.getRespondedAt()).isNotNull();

            verify(friendRequestRepository).save(any(FriendRequest.class));
            verify(eventPublisher).publishEvent(captor.capture());

            Object evt = captor.getValue();
            assertThat(evt).isInstanceOf(iuh.fit.goat.component.realtime.friendship.FriendshipRealtimeEvent.class);
            var fre = (iuh.fit.goat.component.realtime.friendship.FriendshipRealtimeEvent) evt;
            assertThat(fre.getType()).isEqualTo(iuh.fit.goat.enumeration.FriendshipRealtimeEventType.FRIEND_REQUEST_CANCELED);
        }
    }

    @Test
    void handleCancelFriendRequest_saveSetsCanceledStatus() throws Exception {
        FriendRequest request = new FriendRequest();
        request.setRequestId(88L);
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequestStatus.PENDING);

        when(friendRequestRepository.findActiveByIdForUpdate(88L)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("sender@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("sender@example.com")).thenReturn(Optional.of(sender));

            friendRequestService.handleCancelFriendRequest(88L);

            ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
            verify(friendRequestRepository).save(captor.capture());
            FriendRequest saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(FriendRequestStatus.CANCELED);
            assertThat(saved.getRespondedAt()).isNotNull();
        }
    }

    @Test
    void handleCancelFriendRequest_saveThrowsDataIntegrity_thenBubbles() throws Exception {
        FriendRequest request = new FriendRequest();
        request.setRequestId(99L);
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequestStatus.PENDING);

        when(friendRequestRepository.findActiveByIdForUpdate(99L)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenThrow(new DataIntegrityViolationException("dup"));

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("sender@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("sender@example.com")).thenReturn(Optional.of(sender));

            org.junit.jupiter.api.Assertions.assertThrows(DataIntegrityViolationException.class, () ->
                    friendRequestService.handleCancelFriendRequest(99L)
            );
        }
    }
}
