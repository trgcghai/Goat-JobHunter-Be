package iuh.fit.goat.service.impl;

import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.ChatRoomJoinRequestStatus;
import iuh.fit.goat.entity.ChatRoomJoinRequest;
import iuh.fit.goat.repository.ChatRoomJoinRequestRepository;
import iuh.fit.goat.repository.ChatMemberRepository;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChatRoomApproveRejectTests {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomJoinRequestRepository joinRequestRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private MessageService messageService;
    @Mock private iuh.fit.goat.service.helper.ChatRoomPermissionGuard chatRoomPermissionGuard;

    @InjectMocks private ChatRoomServiceImpl chatRoomService;

    private Account moderator;

    @BeforeEach
    void setUp() {
        moderator = new User(); moderator.setAccountId(800L);
    }

    @Test
    void approveJoinRequest_shouldAddMember_whenPending() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(200L); room.setType(iuh.fit.goat.enumeration.ChatRoomType.GROUP);
        ChatRoomJoinRequest req = new ChatRoomJoinRequest(); req.setRequestId(55L); req.setStatus(ChatRoomJoinRequestStatus.PENDING);
        Account requester = new User(); requester.setAccountId(700L); req.setAccount(requester);

        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(200L)).thenReturn(Optional.of(room));
        ChatMember current = new ChatMember(); current.setAccount(moderator); current.setRole(iuh.fit.goat.enumeration.ChatRole.MODERATOR);
        when(chatRoomPermissionGuard.getCurrentMember(room, moderator.getAccountId())).thenReturn(current);
        when(joinRequestRepository.findByIdAndRoomIdForUpdate(55L, 200L)).thenReturn(Optional.of(req));
        when(chatMemberRepository.existsByRoomRoomIdAndAccountAccountIdAndDeletedAtIsNull(200L,700L)).thenReturn(false);

        chatRoomService.approveJoinRequest(moderator, 200L, 55L);

        verify(joinRequestRepository).save(any());
        verify(chatMemberRepository).saveAndFlush(any());
        verify(messageService).createAndSendSystemMessage(eq(200L), any(), any());
    }

    @Test
    void rejectJoinRequest_shouldMarkRejected() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(201L); room.setType(iuh.fit.goat.enumeration.ChatRoomType.GROUP);
        ChatRoomJoinRequest req = new ChatRoomJoinRequest(); req.setRequestId(56L); req.setStatus(ChatRoomJoinRequestStatus.PENDING);

        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(201L)).thenReturn(Optional.of(room));
        ChatMember current2 = new ChatMember(); current2.setAccount(moderator); current2.setRole(iuh.fit.goat.enumeration.ChatRole.MODERATOR);
        when(chatRoomPermissionGuard.getCurrentMember(room, moderator.getAccountId())).thenReturn(current2);
        when(joinRequestRepository.findByIdAndRoomIdForUpdate(56L, 201L)).thenReturn(Optional.of(req));

        chatRoomService.rejectJoinRequest(moderator, 201L, 56L);

        verify(joinRequestRepository).save(any());
    }
}
