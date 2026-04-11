package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.friendship.CreateFriendRequestRequest;
import iuh.fit.goat.dto.response.friendship.FriendRequestResponse;
import iuh.fit.goat.entity.FriendRequest;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.entity.UserRelationship;
import iuh.fit.goat.enumeration.FriendRequestStatus;
import iuh.fit.goat.enumeration.RelationshipState;
import iuh.fit.goat.exception.ConflictException;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.NotFoundException;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.repository.FriendRequestRepository;
import iuh.fit.goat.repository.UserRelationshipRepository;
import iuh.fit.goat.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendRequestServiceImplTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private UserRelationshipRepository userRelationshipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FriendRequestServiceImpl friendRequestService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createFriendRequest_shouldRejectSelfRequest() {
        User current = this.buildUser(1L, "sender@mail.com");
        this.authenticate("sender@mail.com");
        when(this.accountRepository.findByEmailAndDeletedAtIsNull("sender@mail.com")).thenReturn(Optional.of(current));

        CreateFriendRequestRequest request = new CreateFriendRequestRequest(1L);

        InvalidException exception = assertThrows(
                InvalidException.class,
                () -> this.friendRequestService.handleCreateFriendRequest(request)
        );

        assertTrue(exception.getMessage().contains("yourself"));
        verify(this.friendRequestRepository, never()).save(any(FriendRequest.class));
    }

    @Test
    void createFriendRequest_shouldRejectWhenAlreadyFriend() {
        User sender = this.buildUser(1L, "sender@mail.com");
        User receiver = this.buildUser(2L, "receiver@mail.com");

        this.authenticate("sender@mail.com");
        when(this.accountRepository.findByEmailAndDeletedAtIsNull("sender@mail.com")).thenReturn(Optional.of(sender));
        when(this.userRepository.findByAccountIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(receiver));

        UserRelationship relationship = new UserRelationship();
        relationship.setRelationshipState(RelationshipState.FRIEND);
        when(this.userRelationshipRepository.findByPairForUpdate(1L, 2L)).thenReturn(Optional.of(relationship));

        CreateFriendRequestRequest request = new CreateFriendRequestRequest(2L);

        assertThrows(ConflictException.class, () -> this.friendRequestService.handleCreateFriendRequest(request));
        verify(this.friendRequestRepository, never()).save(any(FriendRequest.class));
    }

    @Test
    void createFriendRequest_shouldRejectWhenPendingAlreadyExists() {
        User sender = this.buildUser(1L, "sender@mail.com");
        User receiver = this.buildUser(2L, "receiver@mail.com");

        this.authenticate("sender@mail.com");
        when(this.accountRepository.findByEmailAndDeletedAtIsNull("sender@mail.com")).thenReturn(Optional.of(sender));
        when(this.userRepository.findByAccountIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(receiver));
        when(this.userRelationshipRepository.findByPairForUpdate(1L, 2L)).thenReturn(Optional.empty());
        when(this.friendRequestRepository.existsByPairLowUser_AccountIdAndPairHighUser_AccountIdAndStatusAndDeletedAtIsNull(
                1L,
                2L,
                FriendRequestStatus.PENDING
        )).thenReturn(true);

        CreateFriendRequestRequest request = new CreateFriendRequestRequest(2L);

        assertThrows(ConflictException.class, () -> this.friendRequestService.handleCreateFriendRequest(request));
        verify(this.friendRequestRepository, never()).save(any(FriendRequest.class));
    }

    @Test
    void acceptFriendRequest_shouldCreateFriendRelationshipAndCancelOthers()
            throws InvalidException, ConflictException, NotFoundException {
        User sender = this.buildUser(1L, "sender@mail.com");
        User receiver = this.buildUser(2L, "receiver@mail.com");

        this.authenticate("receiver@mail.com");
        when(this.accountRepository.findByEmailAndDeletedAtIsNull("receiver@mail.com")).thenReturn(Optional.of(receiver));

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setRequestId(10L);
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequest.setPairLowUser(sender);
        friendRequest.setPairHighUser(receiver);
        friendRequest.setStatus(FriendRequestStatus.PENDING);
        friendRequest.setRequestedAt(Instant.now());

        when(this.friendRequestRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(friendRequest));
        when(this.userRelationshipRepository.findByPairForUpdate(1L, 2L)).thenReturn(Optional.empty());
        when(this.friendRequestRepository.save(any(FriendRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FriendRequestResponse response = this.friendRequestService.handleAcceptFriendRequest(10L);

        assertEquals(FriendRequestStatus.ACCEPTED, response.getStatus());
        assertEquals(RelationshipState.FRIEND, response.getRelationshipState());
        verify(this.userRelationshipRepository).upsertFriendRelationship(
                eq(1L),
                eq(2L),
                eq(RelationshipState.FRIEND.name()),
                any(Instant.class),
                eq("receiver@mail.com")
        );
        verify(this.friendRequestRepository).updateStatusForOtherRequests(
                eq(1L),
                eq(2L),
                eq(FriendRequestStatus.PENDING),
                eq(FriendRequestStatus.CANCELED),
                any(Instant.class),
                eq(10L)
        );
        verify(this.eventPublisher).publishEvent(any());
    }

    private void authenticate(String email) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(email, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private User buildUser(Long userId, String email) {
        User user = new User();
        user.setAccountId(userId);
        user.setEmail(email);
        return user;
    }
}
