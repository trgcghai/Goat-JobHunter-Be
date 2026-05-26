package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.MessageEvent;
import iuh.fit.goat.dto.request.chat.AddMemberRequest;
import iuh.fit.goat.dto.request.chat.UpdateMemberRoleRequest;
import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.enumeration.ChatRole;
import iuh.fit.goat.enumeration.ChatRoomType;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.service.*;
import iuh.fit.goat.service.cache.ChatRoomCacheService;
import iuh.fit.goat.service.helper.ChatRoomPermissionGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatRoomMemberRoleManagementTest {

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
    private Account modAccount;
    private Account memberAccount;
    private ChatRoom chatRoom;
    private ChatMember ownerMember;
    private ChatMember modMember;
    private ChatMember memberMember;

    @BeforeEach
    void setUp() throws Exception {
        ownerAccount = new User(); ownerAccount.setAccountId(1L); ownerAccount.setUsername("owner"); ((User)ownerAccount).setFullName("");
        modAccount = new User(); modAccount.setAccountId(2L); modAccount.setUsername("mod"); ((User)modAccount).setFullName("");
        memberAccount = new User(); memberAccount.setAccountId(3L); memberAccount.setUsername("member"); ((User)memberAccount).setFullName("");

        chatRoom = new ChatRoom();
        chatRoom.setRoomId(10L);
        chatRoom.setType(ChatRoomType.GROUP);

        ownerMember = new ChatMember(); ownerMember.setMemberId(101L); ownerMember.setAccount(ownerAccount); ownerMember.setRole(ChatRole.OWNER); ownerMember.setRoom(chatRoom);
        modMember = new ChatMember(); modMember.setMemberId(102L); modMember.setAccount(modAccount); modMember.setRole(ChatRole.MODERATOR); modMember.setRoom(chatRoom);
        memberMember = new ChatMember(); memberMember.setMemberId(103L); memberMember.setAccount(memberAccount); memberMember.setRole(ChatRole.MEMBER); memberMember.setRoom(chatRoom);

        List<ChatMember> members = new ArrayList<>(Arrays.asList(ownerMember, modMember, memberMember));
        chatRoom.setMembers(members);
        when(chatRoomPermissionGuard.getCurrentMember(any(), eq(ownerAccount.getAccountId()))).thenReturn(ownerMember);
        when(chatRoomPermissionGuard.getCurrentMember(any(), eq(modAccount.getAccountId()))).thenReturn(modMember);
        when(chatRoomPermissionGuard.getCurrentMember(any(), eq(memberAccount.getAccountId()))).thenReturn(memberMember);
    }

    @Test
    void ownerCanPromoteMemberToModerator() throws Exception {
        ChatMember target = chatRoom.getMembers().get(2);
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(chatRoom));
        when(chatMemberRepository.findById(any())).thenReturn(Optional.of(target));
        when(chatMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
        req.setRole(ChatRole.MODERATOR);

        ChatMember updated = chatRoomService.updateMemberRole(ownerAccount, 10L, target.getMemberId(), req);
        assertThat(updated.getRole()).isEqualTo(ChatRole.MODERATOR);
        verify(messageService).createAndSendSystemMessage(eq(10L), eq(MessageEvent.ROLE_CHANGED), eq(ownerAccount), any(), any(), any());
    }

    @Test
    void moderatorCannotChangeOwner_throwsInvalid() {
        ChatMember targetOwner = chatRoom.getMembers().get(0);
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(chatRoom));
        when(chatMemberRepository.findById(any())).thenReturn(Optional.of(targetOwner));

        UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
        req.setRole(ChatRole.MEMBER);

        assertThrows(InvalidException.class, () -> chatRoomService.updateMemberRole(modAccount, 10L, targetOwner.getMemberId(), req));
    }

    @Test
    void cannotRemoveLastOwner_throwsInvalid() {
        // remove owner role when only 1 owner exists
        ChatMember targetOwner = chatRoom.getMembers().get(0);
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(chatRoom));
        when(chatMemberRepository.findById(any())).thenReturn(Optional.of(targetOwner));

        UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
        req.setRole(ChatRole.MEMBER);

        assertThrows(InvalidException.class, () -> chatRoomService.updateMemberRole(ownerAccount, 10L, targetOwner.getMemberId(), req));
    }

    @Test
    void nonMemberCannotUpdateRole_throwsInvalid() throws Exception {
        Account outsider = new User(); outsider.setAccountId(99L);
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(any(), eq(99L))).thenThrow(new InvalidException("not a member"));

        UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
        req.setRole(ChatRole.MODERATOR);

        assertThrows(InvalidException.class, () -> chatRoomService.updateMemberRole(outsider, 10L, 999L, req));
    }

    @Test
    void ownerCanRemoveMember_success() throws Exception {
        ChatMember target = chatRoom.getMembers().get(2);
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(chatRoom));
        when(chatMemberRepository.findById(any())).thenReturn(Optional.of(target));

        chatRoomService.removeMemberFromGroup(ownerAccount, 10L, target.getMemberId());

        verify(chatMemberRepository).delete(target);
        verify(messageService).createAndSendSystemMessage(eq(10L), eq(MessageEvent.MEMBER_REMOVED), eq(ownerAccount), any());
    }

    @Test
    void moderatorCannotRemoveOwner_throwsInvalid() {
        ChatMember targetOwner = chatRoom.getMembers().get(0);
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(chatRoom));
        when(chatMemberRepository.findById(any())).thenReturn(Optional.of(targetOwner));

        assertThrows(InvalidException.class, () -> chatRoomService.removeMemberFromGroup(modAccount, 10L, targetOwner.getMemberId()));
    }

    @Test
    void cannotRemoveYourself_throwsInvalid() {
        ChatMember target = chatRoom.getMembers().get(1); // moderator
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(chatRoom));
        when(chatMemberRepository.findById(any())).thenReturn(Optional.of(target));

        assertThrows(InvalidException.class, () -> chatRoomService.removeMemberFromGroup(modAccount, 10L, target.getMemberId()));
    }

    @Test
    void ownerCanAddMember_success() throws Exception {
        AddMemberRequest req = new AddMemberRequest(); req.setAccountId(50L);
        Account newAcc = new User(); newAcc.setAccountId(50L);

        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(chatRoom));
        when(accountRepository.findByAccountIdAndDeletedAtIsNullAndLockedIsFalse(50L)).thenReturn(Optional.of(newAcc));
        when(chatMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        chatRoomService.addMemberToGroup(ownerAccount, 10L, req);

        verify(chatMemberRepository).save(any());
        verify(messageService).createAndSendSystemMessage(eq(10L), eq(MessageEvent.MEMBER_ADDED), eq(ownerAccount), any());
    }

    @Test
    void addMember_alreadyMember_throwsInvalid() {
        AddMemberRequest req = new AddMemberRequest(); req.setAccountId(3L); // memberAccount already in
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(chatRoom));
        when(accountRepository.findByAccountIdAndDeletedAtIsNullAndLockedIsFalse(3L)).thenReturn(Optional.of(memberAccount));

        assertThrows(InvalidException.class, () -> chatRoomService.addMemberToGroup(ownerAccount, 10L, req));
    }

    @Test
    void ownerCannotLeaveGroup_throwsInvalid() {
        when(chatRoomRepository.findByRoomId(10L)).thenReturn(Optional.of(chatRoom));
        assertThrows(InvalidException.class, () -> chatRoomService.leaveGroupChat(ownerAccount, 10L));
    }
}
