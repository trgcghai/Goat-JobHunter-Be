package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.friendship.CreateFriendRequestRequest;
import iuh.fit.goat.dto.response.friendship.FriendRequestResponse;
import iuh.fit.goat.entity.FriendRequest;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.FriendRequestStatus;
import iuh.fit.goat.enumeration.RelationshipState;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.repository.FriendRequestRepository;
import iuh.fit.goat.repository.UserRelationshipRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.util.SecurityUtil;
import iuh.fit.goat.entity.UserRelationship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendRequestServiceImplTest {

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
    void handleCreateFriendRequest_shouldCreatePendingRequest() throws Exception {
        CreateFriendRequestRequest request = new CreateFriendRequestRequest(2L);

        FriendRequest savedRequest = new FriendRequest();
        savedRequest.setRequestId(11L);
        savedRequest.setSender(sender);
        savedRequest.setReceiver(receiver);
        savedRequest.setStatus(FriendRequestStatus.PENDING);
        savedRequest.setRequestedAt(Instant.now());

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("sender@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("sender@example.com")).thenReturn(Optional.of(sender));
            when(userRepository.findByAccountIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(receiver));
            when(userRelationshipRepository.lockPairForTransactionByCanonicalIds(1L, 2L)).thenReturn(new Object());
            when(userRelationshipRepository.findByPairForUpdate(1L, 2L)).thenReturn(Optional.empty());
            when(friendRequestRepository.existsByPairLowUser_AccountIdAndPairHighUser_AccountIdAndStatusAndDeletedAtIsNull(1L, 2L, FriendRequestStatus.PENDING))
                    .thenReturn(false);
            when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(savedRequest);

            FriendRequestResponse response = friendRequestService.handleCreateFriendRequest(request);

            assertThat(response.getRequestId()).isEqualTo(11L);
            assertThat(response.getSenderId()).isEqualTo(1L);
            assertThat(response.getReceiverId()).isEqualTo(2L);
            assertThat(response.getStatus()).isEqualTo(FriendRequestStatus.PENDING);
            verify(friendRequestRepository).save(any(FriendRequest.class));
        }
    }

    @Test
    void handleBlockUser_shouldBlockAndCancelPendingRequests() throws Exception {
        when(userRepository.findByAccountIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(receiver));
        when(userRelationshipRepository.lockPairForTransactionByCanonicalIds(1L, 2L)).thenReturn(new Object());
        when(userRelationshipRepository.upsertBlockedRelationshipByCanonicalIds(
            eq(1L), eq(2L), eq(RelationshipState.BLOCKED.name()), any(Instant.class), eq(1L), eq("sender@example.com")
        )).thenReturn(1);

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("sender@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("sender@example.com")).thenReturn(Optional.of(sender));

            FriendRequestResponse response = friendRequestService.handleBlockUser(2L);

            assertThat(response.getRelationshipState()).isEqualTo(RelationshipState.BLOCKED);
            verify(friendRequestRepository).updateStatusByPair(
                    eq(1L), eq(2L), eq(FriendRequestStatus.PENDING), eq(FriendRequestStatus.CANCELED), any(Instant.class)
            );
        }
    }

    @Test
    void handleCreateFriendRequest_shouldRejectInvalidTargetId() {
        CreateFriendRequestRequest bad = new CreateFriendRequestRequest(null);
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("sender@example.com"));

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                friendRequestService.handleCreateFriendRequest(bad)
            );
        }
    }

    @Test
    void handleCreateFriendRequest_shouldRejectSelfRequest() {
        CreateFriendRequestRequest req = new CreateFriendRequestRequest(1L);
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("sender@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("sender@example.com")).thenReturn(Optional.of(sender));

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                    friendRequestService.handleCreateFriendRequest(req)
            );
        }
    }

    @Test
    void handleCreateFriendRequest_targetNotFoundThrowsNotFound() {
        CreateFriendRequestRequest req = new CreateFriendRequestRequest(99L);
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("sender@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("sender@example.com")).thenReturn(Optional.of(sender));
            when(userRepository.findByAccountIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.NotFoundException.class, () ->
                    friendRequestService.handleCreateFriendRequest(req)
            );
        }
    }

    @Test
    void handleCreateFriendRequest_existingFriendRelationshipThrowsConflict() {
        CreateFriendRequestRequest req = new CreateFriendRequestRequest(2L);
        when(userRepository.findByAccountIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(receiver));
        when(userRelationshipRepository.lockPairForTransactionByCanonicalIds(1L, 2L)).thenReturn(new Object());
        UserRelationship rel = new UserRelationship(); rel.setRelationshipState(iuh.fit.goat.enumeration.RelationshipState.FRIEND);
        when(userRelationshipRepository.findByPairForUpdate(1L,2L)).thenReturn(Optional.of(rel));

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("sender@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("sender@example.com")).thenReturn(Optional.of(sender));

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.ConflictException.class, () ->
                    friendRequestService.handleCreateFriendRequest(req)
            );
        }
    }

    @Test
    void handleCreateFriendRequest_existingBlockedRelationshipThrowsBlockedInteraction() {
        CreateFriendRequestRequest req = new CreateFriendRequestRequest(2L);
        when(userRepository.findByAccountIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(receiver));
        when(userRelationshipRepository.lockPairForTransactionByCanonicalIds(1L, 2L)).thenReturn(new Object());
        UserRelationship rel = new UserRelationship(); rel.setRelationshipState(iuh.fit.goat.enumeration.RelationshipState.BLOCKED);
        when(userRelationshipRepository.findByPairForUpdate(1L,2L)).thenReturn(Optional.of(rel));

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("sender@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("sender@example.com")).thenReturn(Optional.of(sender));

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.BlockedInteractionException.class, () ->
                    friendRequestService.handleCreateFriendRequest(req)
            );
        }
    }

    @Test
    void handleCreateFriendRequest_pendingExistsThrowsConflict() {
        CreateFriendRequestRequest req = new CreateFriendRequestRequest(2L);
        when(userRepository.findByAccountIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(receiver));
        when(userRelationshipRepository.lockPairForTransactionByCanonicalIds(1L, 2L)).thenReturn(new Object());
        when(userRelationshipRepository.findByPairForUpdate(1L,2L)).thenReturn(Optional.empty());
        when(friendRequestRepository.existsByPairLowUser_AccountIdAndPairHighUser_AccountIdAndStatusAndDeletedAtIsNull(1L,2L,iuh.fit.goat.enumeration.FriendRequestStatus.PENDING)).thenReturn(true);

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("sender@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("sender@example.com")).thenReturn(Optional.of(sender));

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.ConflictException.class, () ->
                    friendRequestService.handleCreateFriendRequest(req)
            );
        }
    }

    @Test
    void handleCreateFriendRequest_saveThrowsDataIntegrity_thenConflict() {
        CreateFriendRequestRequest req = new CreateFriendRequestRequest(2L);
        when(userRepository.findByAccountIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(receiver));
        when(userRelationshipRepository.lockPairForTransactionByCanonicalIds(1L, 2L)).thenReturn(new Object());
        when(userRelationshipRepository.findByPairForUpdate(1L,2L)).thenReturn(Optional.empty());
        when(friendRequestRepository.existsByPairLowUser_AccountIdAndPairHighUser_AccountIdAndStatusAndDeletedAtIsNull(1L,2L,iuh.fit.goat.enumeration.FriendRequestStatus.PENDING)).thenReturn(false);
        when(friendRequestRepository.save(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup"));

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("sender@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("sender@example.com")).thenReturn(Optional.of(sender));

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.ConflictException.class, () ->
                    friendRequestService.handleCreateFriendRequest(req)
            );
        }
    }

    @Test
    void handleAcceptFriendRequest_shouldOnlyReceiver() throws Exception {
        FriendRequest pending = new FriendRequest(); pending.setRequestId(77L); pending.setSender(sender); pending.setReceiver(receiver); pending.setStatus(iuh.fit.goat.enumeration.FriendRequestStatus.PENDING);
        when(friendRequestRepository.findActiveById(77L)).thenReturn(Optional.of(pending));
        when(friendRequestRepository.findActiveByIdForUpdate(77L)).thenReturn(Optional.of(pending));
        when(userRelationshipRepository.lockPairForTransactionByCanonicalIds(anyLong(), anyLong())).thenReturn(new Object());

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            // current user is sender -> cannot accept
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("sender@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("sender@example.com")).thenReturn(Optional.of(sender));

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                    friendRequestService.handleAcceptFriendRequest(77L)
            );

            // now current user is receiver -> success path (simulate save)
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("receiver@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("receiver@example.com")).thenReturn(Optional.of(receiver));
            when(friendRequestRepository.save(any(FriendRequest.class))).thenAnswer(i -> i.getArgument(0));

            var resp = friendRequestService.handleAcceptFriendRequest(77L);
            assertThat(resp.getStatus()).isEqualTo(iuh.fit.goat.enumeration.FriendRequestStatus.ACCEPTED);
        }
    }
}