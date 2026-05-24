package iuh.fit.goat.service.impl;

import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.ChatRole;
import iuh.fit.goat.enumeration.ChatRoomType;
import iuh.fit.goat.common.MessageEvent;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.ChatMemberRepository;
import iuh.fit.goat.service.MessageService;
import iuh.fit.goat.service.NotificationService;
import iuh.fit.goat.service.helper.ChatRoomPermissionGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomDissolutionLeaveTests {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private MessageService messageService;
    @Mock private NotificationService notificationService;
    @Mock private ChatRoomPermissionGuard chatRoomPermissionGuard;

    @InjectMocks private ChatRoomServiceImpl chatRoomService;

    private Account owner;

    @BeforeEach
    void setUp() {
        owner = new User(); owner.setAccountId(4000L); owner.setEmail("owner@example.com");
    }

    @Test
    void leaveGroupChat_ownerCannotLeave() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(500L); room.setType(ChatRoomType.GROUP);
        ChatMember member = new ChatMember(); member.setAccount(owner); member.setRole(ChatRole.OWNER);

        when(chatRoomRepository.findByRoomId(500L)).thenReturn(Optional.of(room));
        when(chatRoomPermissionGuard.getCurrentMember(room, owner.getAccountId())).thenReturn(member);

        assertThatThrownBy(() -> chatRoomService.leaveGroupChat(owner, 500L))
                .isInstanceOf(iuh.fit.goat.exception.InvalidException.class);
    }

    @Test
    void leaveGroupChat_memberLeavesSuccessfully() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(501L); room.setType(ChatRoomType.GROUP);
        Account memberAcc = new User(); memberAcc.setAccountId(4001L); memberAcc.setEmail("m1@example.com");
        ChatMember member = new ChatMember(); member.setAccount(memberAcc); member.setRole(ChatRole.MEMBER);

        when(chatRoomRepository.findByRoomId(501L)).thenReturn(Optional.of(room));
        when(chatRoomPermissionGuard.getCurrentMember(room, memberAcc.getAccountId())).thenReturn(member);
        when(chatMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        chatRoomService.leaveGroupChat(memberAcc, 501L);

        verify(chatMemberRepository).save(member);
        verify(messageService).createAndSendSystemMessage(eq(501L), any(MessageEvent.class), eq(memberAcc));
    }

    @Test
    void smartGroupDissolution_onlyOwnerAllowed_and_nameMustMatch() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(600L); room.setType(ChatRoomType.GROUP); room.setName("MyGroup");
        ChatMember mod = new ChatMember(); mod.setAccount(owner); mod.setRole(ChatRole.MODERATOR);

        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(600L)).thenReturn(Optional.of(room));
        when(chatRoomPermissionGuard.getCurrentMember(room, owner.getAccountId())).thenReturn(mod);

        // not owner
        assertThatThrownBy(() -> chatRoomService.smartGroupDissolution(600L, owner, "MyGroup"))
                .isInstanceOf(iuh.fit.goat.exception.InvalidException.class);

        // owner but name mismatch
        ChatMember ownerMember = new ChatMember(); ownerMember.setAccount(owner); ownerMember.setRole(ChatRole.OWNER);
        room.setMembers(List.of(ownerMember));
        when(chatRoomPermissionGuard.getCurrentMember(room, owner.getAccountId())).thenReturn(ownerMember);

        assertThatThrownBy(() -> chatRoomService.smartGroupDissolution(600L, owner, "WrongName"))
                .isInstanceOf(iuh.fit.goat.exception.InvalidException.class);
    }

    @Test
    void smartGroupDissolution_happyPath_dissolvesAndNotifiesMembers() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(601L); room.setType(ChatRoomType.GROUP); room.setName("TeamX");
        Account a1 = new User(); a1.setAccountId(10L); a1.setEmail("a1@example.com");
        Account a2 = new User(); a2.setAccountId(11L); a2.setEmail("a2@example.com");
        ChatMember m1 = new ChatMember(); m1.setAccount(a1); m1.setRole(ChatRole.MEMBER);
        ChatMember m2 = new ChatMember(); m2.setAccount(a2); m2.setRole(ChatRole.MEMBER);
        ChatMember ownerMember = new ChatMember(); ownerMember.setAccount(owner); ownerMember.setRole(ChatRole.OWNER);
        room.setMembers(List.of(ownerMember, m1, m2));

        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(601L)).thenReturn(Optional.of(room));
        when(chatRoomPermissionGuard.getCurrentMember(room, owner.getAccountId())).thenReturn(ownerMember);
        when(chatRoomRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(chatMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        chatRoomService.smartGroupDissolution(601L, owner, "TeamX");

        verify(chatRoomRepository).save(room);
        verify(chatMemberRepository).save(ownerMember);
        verify(messageService).createAndSendSystemMessage(eq(601L), eq(MessageEvent.GROUP_DISSOLVED), eq(owner));
        verify(notificationService, times(1)).handleSendMessageTextToUser(eq("a1@example.com"), anyString());
        verify(notificationService, times(1)).handleSendMessageTextToUser(eq("a2@example.com"), anyString());
    }

    
}
