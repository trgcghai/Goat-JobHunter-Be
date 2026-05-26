package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.MessageEvent;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.ChatRole;
import iuh.fit.goat.enumeration.ChatRoomType;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.ChatMemberRepository;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.service.MessageService;
import iuh.fit.goat.service.helper.ChatRoomPermissionGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatRoomLeaveGroupTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private MessageService messageService;
    @Mock private ChatRoomPermissionGuard chatRoomPermissionGuard;

    @InjectMocks private ChatRoomServiceImpl chatRoomService;

    private Account owner;

    @BeforeEach
    void setUp() {
        owner = new User(); owner.setAccountId(4000L); owner.setEmail("owner@example.com");
    }

    @Test
    void ownerCannotLeave_throwsInvalid() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(700L); room.setType(ChatRoomType.GROUP);
        ChatMember ownerMember = new ChatMember(); ownerMember.setAccount(owner); ownerMember.setRole(ChatRole.OWNER);

        when(chatRoomRepository.findByRoomId(700L)).thenReturn(Optional.of(room));
        when(chatRoomPermissionGuard.getCurrentMember(room, owner.getAccountId())).thenReturn(ownerMember);

        assertThrows(InvalidException.class, () -> chatRoomService.leaveGroupChat(owner, 700L));
    }

    @Test
    void memberLeavesSuccessfully_callsSaveAndSendsMessage() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(701L); room.setType(ChatRoomType.GROUP);
        Account memberAcc = new User(); memberAcc.setAccountId(7011L); memberAcc.setEmail("m1@example.com");
        ChatMember member = new ChatMember(); member.setAccount(memberAcc); member.setRole(ChatRole.MEMBER);

        when(chatRoomRepository.findByRoomId(701L)).thenReturn(Optional.of(room));
        when(chatRoomPermissionGuard.getCurrentMember(room, memberAcc.getAccountId())).thenReturn(member);
        when(chatMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        chatRoomService.leaveGroupChat(memberAcc, 701L);

        verify(chatMemberRepository).save(member);
        verify(chatMemberRepository).flush();
        verify(messageService).createAndSendSystemMessage(eq(701L), eq(MessageEvent.MEMBER_LEFT), eq(memberAcc));
    }

    @Test
    void moderatorLeavesSuccessfully_allowed() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(702L); room.setType(ChatRoomType.GROUP);
        Account modAcc = new User(); modAcc.setAccountId(7021L); modAcc.setEmail("mod@example.com");
        ChatMember mod = new ChatMember(); mod.setAccount(modAcc); mod.setRole(ChatRole.MODERATOR);

        when(chatRoomRepository.findByRoomId(702L)).thenReturn(Optional.of(room));
        when(chatRoomPermissionGuard.getCurrentMember(room, modAcc.getAccountId())).thenReturn(mod);
        when(chatMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        chatRoomService.leaveGroupChat(modAcc, 702L);

        verify(chatMemberRepository).save(mod);
        verify(messageService).createAndSendSystemMessage(eq(702L), eq(MessageEvent.MEMBER_LEFT), eq(modAcc));
    }

    @Test
    void leave_nonexistentChat_throwsInvalid() {
        when(chatRoomRepository.findByRoomId(800L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.leaveGroupChat(owner, 800L))
                .isInstanceOf(iuh.fit.goat.exception.InvalidException.class);
    }

    @Test
    void leave_nonGroupChat_throwsInvalid() {
        ChatRoom room = new ChatRoom(); room.setRoomId(703L); room.setType(ChatRoomType.DIRECT);
        when(chatRoomRepository.findByRoomId(703L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> chatRoomService.leaveGroupChat(owner, 703L))
                .isInstanceOf(iuh.fit.goat.exception.InvalidException.class);
    }

    @Test
    void leave_memberNotFound_throwsInvalidFromGuard() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(704L); room.setType(ChatRoomType.GROUP);
        when(chatRoomRepository.findByRoomId(704L)).thenReturn(Optional.of(room));
        when(chatRoomPermissionGuard.getCurrentMember(room, owner.getAccountId())).thenThrow(new InvalidException("Not a member"));

        assertThatThrownBy(() -> chatRoomService.leaveGroupChat(owner, 704L))
                .isInstanceOf(iuh.fit.goat.exception.InvalidException.class);
    }

    @Test
    void leave_setsDeletedAt_onMember() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(705L); room.setType(ChatRoomType.GROUP);
        Account a = new User(); a.setAccountId(7051L); a.setEmail("u@example.com");
        ChatMember member = new ChatMember(); member.setAccount(a); member.setRole(ChatRole.MEMBER);

        when(chatRoomRepository.findByRoomId(705L)).thenReturn(Optional.of(room));
        when(chatRoomPermissionGuard.getCurrentMember(room, a.getAccountId())).thenReturn(member);
        when(chatMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        chatRoomService.leaveGroupChat(a, 705L);

        assertThat(member.getDeletedAt()).isNotNull();
    }

    @Test
    void leave_callsFlush_afterSave() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(706L); room.setType(ChatRoomType.GROUP);
        Account a = new User(); a.setAccountId(7061L); a.setEmail("u2@example.com");
        ChatMember member = new ChatMember(); member.setAccount(a); member.setRole(ChatRole.MEMBER);

        when(chatRoomRepository.findByRoomId(706L)).thenReturn(Optional.of(room));
        when(chatRoomPermissionGuard.getCurrentMember(room, a.getAccountId())).thenReturn(member);
        when(chatMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        chatRoomService.leaveGroupChat(a, 706L);

        verify(chatMemberRepository).save(member);
        verify(chatMemberRepository).flush();
    }

    @Test
    void leave_sendsMemberLeftEvent_once() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(707L); room.setType(ChatRoomType.GROUP);
        Account a = new User(); a.setAccountId(7071L); a.setEmail("u3@example.com");
        ChatMember member = new ChatMember(); member.setAccount(a); member.setRole(ChatRole.MEMBER);

        when(chatRoomRepository.findByRoomId(707L)).thenReturn(Optional.of(room));
        when(chatRoomPermissionGuard.getCurrentMember(room, a.getAccountId())).thenReturn(member);
        when(chatMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        chatRoomService.leaveGroupChat(a, 707L);

        verify(messageService, times(1)).createAndSendSystemMessage(eq(707L), eq(MessageEvent.MEMBER_LEFT), eq(a));
    }
}
