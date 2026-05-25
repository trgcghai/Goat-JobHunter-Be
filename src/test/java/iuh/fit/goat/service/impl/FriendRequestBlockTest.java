package iuh.fit.goat.service.impl;

import iuh.fit.goat.component.realtime.friendship.FriendshipRealtimeEvent;
import iuh.fit.goat.dto.response.friendship.FriendRequestResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.FriendRequestStatus;
import iuh.fit.goat.enumeration.FriendshipRealtimeEventType;
import iuh.fit.goat.enumeration.RelationshipState;
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

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendRequestBlockTest {

    @Mock private FriendRequestRepository friendRequestRepository;
    @Mock private UserRelationshipRepository userRelationshipRepository;
    @Mock private UserRepository userRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FriendRequestServiceImpl friendRequestService;

    private Role role;
    private User actor;
    private User target;
    private User higherActor;
    private User lowerTarget;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setRoleId(1L);
        role.setName("USER");

        actor = new User();
        actor.setAccountId(1L);
        actor.setEmail("actor@example.com");
        actor.setUsername("actor");
        actor.setPassword("hash");
        actor.setRole(role);

        target = new User();
        target.setAccountId(2L);
        target.setEmail("target@example.com");
        target.setUsername("target");
        target.setPassword("hash");
        target.setRole(role);

        higherActor = new User();
        higherActor.setAccountId(10L);
        higherActor.setEmail("higher@example.com");
        higherActor.setUsername("higher");
        higherActor.setPassword("hash");
        higherActor.setRole(role);

        lowerTarget = new User();
        lowerTarget.setAccountId(3L);
        lowerTarget.setEmail("lower@example.com");
        lowerTarget.setUsername("lower");
        lowerTarget.setPassword("hash");
        lowerTarget.setRole(role);
    }

    @Test
    void handleBlockUser_nullTargetId_throwsInvalid() {
        org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                friendRequestService.handleBlockUser(null)
        );
    }

    @Test
    void handleBlockUser_negativeTargetId_throwsInvalid() {
        org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                friendRequestService.handleBlockUser(-1L)
        );
    }

    @Test
    void handleBlockUser_selfBlock_throwsInvalid() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("actor@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("actor@example.com")).thenReturn(Optional.of(actor));

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                    friendRequestService.handleBlockUser(1L)
            );
        }
    }

    @Test
    void handleBlockUser_unauthenticated_throwsInvalid() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.empty());

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                    friendRequestService.handleBlockUser(2L)
            );
        }
    }

    @Test
    void handleBlockUser_currentAccountMissing_throwsInvalid() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("actor@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("actor@example.com")).thenReturn(Optional.empty());

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                    friendRequestService.handleBlockUser(2L)
            );
        }
    }

    @Test
    void handleBlockUser_currentAccountIsNotUser_throwsInvalid() {
        Company companyAccount = new Company();
        companyAccount.setAccountId(99L);
        companyAccount.setEmail("company@example.com");
        companyAccount.setUsername("company");
        companyAccount.setPassword("hash");
        companyAccount.setName("Company");

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("company@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("company@example.com")).thenReturn(Optional.of(companyAccount));

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                    friendRequestService.handleBlockUser(2L)
            );
        }
    }

    @Test
    void handleBlockUser_targetNotFound_throwsNotFound() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("actor@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("actor@example.com")).thenReturn(Optional.of(actor));
            when(userRepository.findByAccountIdAndDeletedAtIsNull(2L)).thenReturn(Optional.empty());

            org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.NotFoundException.class, () ->
                    friendRequestService.handleBlockUser(2L)
            );
        }
    }

    @Test
    void handleBlockUser_success_lowToHighPair_updatesBlockAndCancelsPendingRequests() throws Exception {
        when(userRepository.findByAccountIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(target));
        when(userRelationshipRepository.lockPairForTransactionByCanonicalIds(1L, 2L)).thenReturn(new Object());

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("actor@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("actor@example.com")).thenReturn(Optional.of(actor));

            FriendRequestResponse response = friendRequestService.handleBlockUser(2L);

            assertThat(response.getSenderId()).isEqualTo(1L);
            assertThat(response.getReceiverId()).isEqualTo(2L);
            assertThat(response.getRelationshipState()).isEqualTo(RelationshipState.BLOCKED);
            assertThat(response.getRequestedAt()).isNotNull();

            verify(userRelationshipRepository).upsertBlockedRelationshipByCanonicalIds(
                    eq(1L), eq(2L), eq(RelationshipState.BLOCKED.name()), any(Instant.class), eq(1L), eq("actor@example.com")
            );
            verify(friendRequestRepository).updateStatusByPair(
                    eq(1L), eq(2L), eq(FriendRequestStatus.PENDING), eq(FriendRequestStatus.CANCELED), any(Instant.class)
            );
        }
    }

    @Test
    void handleBlockUser_success_highToLowPair_usesCanonicalOrdering() throws Exception {
        when(userRepository.findByAccountIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(lowerTarget));
        when(userRelationshipRepository.lockPairForTransactionByCanonicalIds(3L, 10L)).thenReturn(new Object());

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("higher@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("higher@example.com")).thenReturn(Optional.of(higherActor));

            FriendRequestResponse response = friendRequestService.handleBlockUser(3L);

            assertThat(response.getSenderId()).isEqualTo(10L);
            assertThat(response.getReceiverId()).isEqualTo(3L);
            assertThat(response.getRelationshipState()).isEqualTo(RelationshipState.BLOCKED);

            verify(userRelationshipRepository).upsertBlockedRelationshipByCanonicalIds(
                    eq(3L), eq(10L), eq(RelationshipState.BLOCKED.name()), any(Instant.class), eq(10L), eq("higher@example.com")
            );
            verify(friendRequestRepository).updateStatusByPair(
                    eq(3L), eq(10L), eq(FriendRequestStatus.PENDING), eq(FriendRequestStatus.CANCELED), any(Instant.class)
            );
        }
    }

    @Test
    void handleBlockUser_eventPublished_containsBlockedPayload() throws Exception {
        when(userRepository.findByAccountIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(target));
        when(userRelationshipRepository.lockPairForTransactionByCanonicalIds(1L, 2L)).thenReturn(new Object());

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("actor@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("actor@example.com")).thenReturn(Optional.of(actor));

            ArgumentCaptor<FriendshipRealtimeEvent> captor = ArgumentCaptor.forClass(FriendshipRealtimeEvent.class);

            friendRequestService.handleBlockUser(2L);

            verify(eventPublisher).publishEvent(captor.capture());
            FriendshipRealtimeEvent event = captor.getValue();

            assertThat(event.getType()).isEqualTo(FriendshipRealtimeEventType.USER_BLOCKED);
            assertThat(event.getRequestId()).isNull();
            assertThat(event.getRelationshipState()).isEqualTo(RelationshipState.BLOCKED);
            assertThat(event.getActorPrincipal()).isEqualTo("actor@example.com");
            assertThat(event.getTargetPrincipal()).isEqualTo("target@example.com");
        }
    }

    @Test
    void handleBlockUser_responseUsesCurrentActorAndTargetIds() throws Exception {
        when(userRepository.findByAccountIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(target));
        when(userRelationshipRepository.lockPairForTransactionByCanonicalIds(1L, 2L)).thenReturn(new Object());

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("actor@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("actor@example.com")).thenReturn(Optional.of(actor));

            FriendRequestResponse response = friendRequestService.handleBlockUser(2L);

            assertThat(response.getSenderId()).isEqualTo(1L);
            assertThat(response.getReceiverId()).isEqualTo(2L);
            assertThat(response.getRelationshipState()).isEqualTo(RelationshipState.BLOCKED);
            assertThat(response.getRequestedAt()).isNotNull();
        }
    }
}