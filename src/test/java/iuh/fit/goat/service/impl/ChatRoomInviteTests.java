package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.chat.InviteLinkResponse;
import iuh.fit.goat.dto.response.chat.InviteTokenPreviewResponse;
import iuh.fit.goat.dto.response.chat.JoinByInviteResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.ChatRoomPrivacy;
import iuh.fit.goat.enumeration.ChatRole;
import iuh.fit.goat.enumeration.ChatRoomType;
import iuh.fit.goat.enumeration.ChatRoomJoinRequestStatus;
import iuh.fit.goat.entity.ChatRoomJoinRequest;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomInviteTests {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private ChatRoomJoinRequestRepository chatRoomJoinRequestRepository;
    @Mock private iuh.fit.goat.service.helper.ChatRoomPermissionGuard chatRoomPermissionGuard;
    @Mock private MessageService messageService;

    @InjectMocks private ChatRoomServiceImpl chatRoomService;

    private Account owner;

    @BeforeEach
    void setUp() {
        owner = new User(); owner.setAccountId(900L);
    }

    @Test
    void getInviteLink_shouldReturnLinkForMember() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(1000L); room.setInviteToken("tok1"); room.setInviteEnabled(true); room.setType(ChatRoomType.GROUP);
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(1000L)).thenReturn(Optional.of(room));
        ChatMember member = new ChatMember(); member.setAccount(owner); member.setRole(ChatRole.OWNER);
        when(chatRoomPermissionGuard.getCurrentMember(room, owner.getAccountId())).thenReturn(member);

        InviteLinkResponse resp = chatRoomService.getInviteLink(owner, 1000L);
        assertThat(resp.getInviteToken()).isEqualTo("tok1");
        assertThat(resp.isInviteEnabled()).isTrue();
        assertThat(resp.getInviteLink()).contains("tok1");
    }

    @Test
    void rotateInviteLink_shouldChangeTokenForOwner() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(1010L); room.setInviteToken("old"); room.setInviteEnabled(true); room.setType(ChatRoomType.GROUP);
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(1010L)).thenReturn(Optional.of(room));
        ChatMember member = new ChatMember(); member.setAccount(owner); member.setRole(ChatRole.OWNER);
        when(chatRoomPermissionGuard.getCurrentMember(room, owner.getAccountId())).thenReturn(member);
        when(chatRoomRepository.existsByInviteToken(any())).thenReturn(false);
        when(chatRoomRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        InviteLinkResponse resp = chatRoomService.rotateInviteLink(owner, 1010L);
        assertThat(resp.getInviteToken()).isNotNull();
        assertThat(resp.getInviteToken()).isNotEqualTo("old");
        assertThat(resp.getInviteRotatedAt()).isNotNull();
    }

    @Test
    void toggleInviteLink_shouldUpdateEnabledFlag() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(1020L); room.setInviteToken("t"); room.setInviteEnabled(true); room.setType(ChatRoomType.GROUP);
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(1020L)).thenReturn(Optional.of(room));
        ChatMember member = new ChatMember(); member.setAccount(owner); member.setRole(ChatRole.OWNER);
        when(chatRoomPermissionGuard.getCurrentMember(room, owner.getAccountId())).thenReturn(member);
        when(chatRoomRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        InviteLinkResponse resp = chatRoomService.toggleInviteLink(owner, 1020L, false);
        assertThat(resp.isInviteEnabled()).isFalse();
    }

    @Test
    void getInvitePreview_shouldReturnPreview_whenTokenValid() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(2000L); room.setName("G"); room.setInviteEnabled(true); room.setPrivacy(ChatRoomPrivacy.PUBLIC); room.setType(ChatRoomType.GROUP);
        when(chatRoomRepository.findByInviteTokenAndDeletedAtIsNull("tokx")).thenReturn(Optional.of(room));

        InviteTokenPreviewResponse resp = chatRoomService.getInvitePreview("tokx");
        assertThat(resp.getRoomId()).isEqualTo(2000L);
        assertThat(resp.isInviteEnabled()).isTrue();
    }

    @Test
    void getInvitePreview_shouldThrowWhenDisabled() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(2001L); room.setInviteEnabled(false); room.setType(ChatRoomType.GROUP);
        when(chatRoomRepository.findByInviteTokenAndDeletedAtIsNull("toky")).thenReturn(Optional.of(room));

        assertThrows(iuh.fit.goat.exception.NotFoundException.class, () -> chatRoomService.getInvitePreview("toky"));
    }

    @Test
    void joinByInvite_privateRoom_shouldCreatePendingRequest() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(3000L); room.setInviteToken("pvt"); room.setInviteEnabled(true); room.setPrivacy(ChatRoomPrivacy.PRIVATE); room.setType(ChatRoomType.GROUP);
        when(chatRoomRepository.findByInviteTokenAndDeletedAtIsNull("pvt")).thenReturn(Optional.of(room));
        Account user = new User(); user.setAccountId(3001L);
        when(chatMemberRepository.existsByRoomRoomIdAndAccountAccountIdAndDeletedAtIsNull(3000L, 3001L)).thenReturn(false);
        when(chatRoomJoinRequestRepository.findByRoomRoomIdAndAccountAccountIdAndStatusAndDeletedAtIsNull(3000L,3001L, ChatRoomJoinRequestStatus.PENDING)).thenReturn(Optional.empty());
        when(chatRoomJoinRequestRepository.save(any())).thenAnswer(i -> { ChatRoomJoinRequest r = i.getArgument(0); r.setRequestId(77L); return r;});

        JoinByInviteResponse resp = chatRoomService.joinByInvite(user, "pvt");
        assertThat(resp.isJoined()).isFalse();
        assertThat(resp.getStatus()).isEqualTo("request_pending");
        assertThat(resp.getRequestId()).isNotNull();
    }
}
