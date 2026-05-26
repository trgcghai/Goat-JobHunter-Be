package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.MessageEvent;
import iuh.fit.goat.dto.response.chat.InviteLinkResponse;
import iuh.fit.goat.dto.response.chat.InviteTokenPreviewResponse;
import iuh.fit.goat.dto.response.chat.JoinByInviteResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.ChatRole;
import iuh.fit.goat.enumeration.ChatRoomPrivacy;
import iuh.fit.goat.enumeration.ChatRoomType;
import iuh.fit.goat.enumeration.ChatRoomJoinRequestStatus;
import iuh.fit.goat.exception.ConflictException;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.NotFoundException;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.service.*;
import iuh.fit.goat.service.cache.ChatRoomCacheService;
import iuh.fit.goat.service.helper.ChatRoomPermissionGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatRoomInviteLinkQrManagementTest {

    @Mock private MessageService messageService;
    @Mock private NotificationService notificationService;
    @Mock private AiService aiService;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private ChatRoomJoinRequestRepository chatRoomJoinRequestRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private UserRelationshipRepository userRelationshipRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private ChatRoomCacheService chatRoomCacheService;
    @Mock private ChatRoomPermissionGuard chatRoomPermissionGuard;

    @InjectMocks private ChatRoomServiceImpl chatRoomService;

    private Account ownerAccount;
    private Account memberAccount;
    private ChatRoom room;
    private ChatMember ownerMember;
    private ChatMember memberMember;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(chatRoomService, "frontendBaseUrl", "https://frontend.example.com");

        ownerAccount = new User();
        ownerAccount.setAccountId(1L);
        ownerAccount.setUsername("owner");
        ((User) ownerAccount).setFullName("");

        memberAccount = new User();
        memberAccount.setAccountId(2L);
        memberAccount.setUsername("member");
        ((User) memberAccount).setFullName("");

        room = new ChatRoom();
        room.setRoomId(100L);
        room.setType(ChatRoomType.GROUP);
        room.setName("Group A");
        room.setPrivacy(ChatRoomPrivacy.PUBLIC);
        room.setInviteToken("token-100");
        room.setInviteEnabled(true);
        room.setInviteRotatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        ownerMember = new ChatMember();
        ownerMember.setMemberId(11L);
        ownerMember.setAccount(ownerAccount);
        ownerMember.setRole(ChatRole.OWNER);
        ownerMember.setRoom(room);

        memberMember = new ChatMember();
        memberMember.setMemberId(12L);
        memberMember.setAccount(memberAccount);
        memberMember.setRole(ChatRole.MEMBER);
        memberMember.setRoom(room);

        room.setMembers(List.of(ownerMember, memberMember));

        when(chatRoomPermissionGuard.getCurrentMember(any(), eq(ownerAccount.getAccountId()))).thenReturn(ownerMember);
        when(chatRoomPermissionGuard.getCurrentMember(any(), eq(memberAccount.getAccountId()))).thenReturn(memberMember);
    }

    @Test
    void getInviteLink_success_buildsFrontendInviteUrl() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));

        InviteLinkResponse response = chatRoomService.getInviteLink(ownerAccount, 100L);

        assertThat(response.getRoomId()).isEqualTo(100L);
        assertThat(response.getInviteToken()).isEqualTo("token-100");
        assertThat(response.getInviteLink()).isEqualTo("https://frontend.example.com/invite/token-100");
        assertThat(response.isInviteEnabled()).isTrue();
        assertThat(response.getPrivacy()).isEqualTo(ChatRoomPrivacy.PUBLIC);
    }

    @Test
    void getInviteLink_nonMember_throwsInvalid() throws Exception {
        Account outsider = new User();
        outsider.setAccountId(99L);
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));
        when(chatRoomPermissionGuard.getCurrentMember(any(), eq(99L))).thenThrow(new InvalidException("not member"));

        assertThrows(InvalidException.class, () -> chatRoomService.getInviteLink(outsider, 100L));
    }

    @Test
    void getInviteLink_nonGroupRoom_throwsInvalid() throws Exception {
        room.setType(ChatRoomType.DIRECT);
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));

        assertThrows(InvalidException.class, () -> chatRoomService.getInviteLink(ownerAccount, 100L));
    }

    @Test
    void rotateInviteLink_success_changesTokenAndLink() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));
        when(chatRoomRepository.existsByInviteToken(any())).thenReturn(false);
        when(chatRoomRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InviteLinkResponse response = chatRoomService.rotateInviteLink(ownerAccount, 100L);

        assertThat(response.getRoomId()).isEqualTo(100L);
        assertThat(response.getInviteToken()).isNotEqualTo("token-100");
        assertThat(response.getInviteLink()).startsWith("https://frontend.example.com/invite/");
        assertThat(response.getInviteRotatedAt()).isNotNull();
    }

    @Test
    void rotateInviteLink_nonOwner_throwsInvalid() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));
        when(chatRoomPermissionGuard.getCurrentMember(any(), eq(memberAccount.getAccountId()))).thenReturn(memberMember);

        assertThrows(InvalidException.class, () -> chatRoomService.rotateInviteLink(memberAccount, 100L));
    }

    @Test
    void toggleInviteLink_disable_success() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));
        when(chatRoomRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InviteLinkResponse response = chatRoomService.toggleInviteLink(ownerAccount, 100L, false);

        assertThat(response.isInviteEnabled()).isFalse();
        assertThat(response.getInviteLink()).isEqualTo("https://frontend.example.com/invite/token-100");
    }

    @Test
    void toggleInviteLink_nonOwner_throwsInvalid() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));
        when(chatRoomPermissionGuard.getCurrentMember(any(), eq(memberAccount.getAccountId()))).thenReturn(memberMember);

        assertThrows(InvalidException.class, () -> chatRoomService.toggleInviteLink(memberAccount, 100L, false));
    }

    @Test
    void getInvitePreview_success_returnsRoomInfo() throws Exception {
        when(chatRoomRepository.findByInviteTokenAndDeletedAtIsNull("token-100")).thenReturn(Optional.of(room));

        InviteTokenPreviewResponse response = chatRoomService.getInvitePreview("token-100");

        assertThat(response.getRoomId()).isEqualTo(100L);
        assertThat(response.getRoomName()).isEqualTo("Group A");
        assertThat(response.isInviteEnabled()).isTrue();
        assertThat(response.getPrivacy()).isEqualTo(ChatRoomPrivacy.PUBLIC);
    }

    @Test
    void getInvitePreview_disabledInvite_throwsNotFound() throws Exception {
        room.setInviteEnabled(false);
        when(chatRoomRepository.findByInviteTokenAndDeletedAtIsNull("token-100")).thenReturn(Optional.of(room));

        assertThrows(NotFoundException.class, () -> chatRoomService.getInvitePreview("token-100"));
    }

    @Test
    void joinByInvite_publicRoom_success_joinsDirectly() throws Exception {
        when(chatRoomRepository.findByInviteTokenAndDeletedAtIsNull("token-100")).thenReturn(Optional.of(room));
        when(chatMemberRepository.existsByRoomRoomIdAndAccountAccountIdAndDeletedAtIsNull(100L, 2L)).thenReturn(false);
        when(chatMemberRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        JoinByInviteResponse response = chatRoomService.joinByInvite(memberAccount, "token-100");

        assertThat(response.isJoined()).isTrue();
        assertThat(response.getStatus()).isEqualTo("joined");
        verify(messageService).createAndSendSystemMessage(eq(100L), eq(MessageEvent.MEMBER_JOINED_BY_INVITE), eq(memberAccount));
    }

    @Test
    void joinByInvite_privateRoom_pendingRequest_created() throws Exception {
        room.setPrivacy(ChatRoomPrivacy.PRIVATE);
        when(chatRoomRepository.findByInviteTokenAndDeletedAtIsNull("token-100")).thenReturn(Optional.of(room));
        when(chatMemberRepository.existsByRoomRoomIdAndAccountAccountIdAndDeletedAtIsNull(eq(100L), eq(2L))).thenReturn(false);
        when(chatRoomJoinRequestRepository.findByRoomRoomIdAndAccountAccountIdAndStatusAndDeletedAtIsNull(
            eq(100L), eq(2L), eq(ChatRoomJoinRequestStatus.PENDING))).thenReturn(Optional.empty());
        when(chatRoomJoinRequestRepository.save(any())).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            ((iuh.fit.goat.entity.ChatRoomJoinRequest) arg).setRequestId(555L);
            return arg;
        });

        JoinByInviteResponse response = chatRoomService.joinByInvite(memberAccount, "token-100");

        assertThat(response.isJoined()).isFalse();
        assertThat(response.getStatus()).isEqualTo("request_pending");
        assertThat(response.getRequestId()).isEqualTo(555L);
    }

    @Test
    void joinByInvite_existingMember_throwsConflict() throws Exception {
        when(chatRoomRepository.findByInviteTokenAndDeletedAtIsNull("token-100")).thenReturn(Optional.of(room));
        when(chatMemberRepository.existsByRoomRoomIdAndAccountAccountIdAndDeletedAtIsNull(100L, 2L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> chatRoomService.joinByInvite(memberAccount, "token-100"));
    }
}
