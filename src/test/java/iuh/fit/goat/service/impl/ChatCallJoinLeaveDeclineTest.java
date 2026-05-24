package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.chat.JoinChatCallRequest;
import iuh.fit.goat.dto.request.chat.StartChatCallRequest;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.ChatCallSessionStatus;
import iuh.fit.goat.enumeration.ChatCallType;
import iuh.fit.goat.enumeration.ChatRoomType;
import iuh.fit.goat.repository.ChatCallParticipantRepository;
import iuh.fit.goat.repository.ChatCallSessionRepository;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.service.ChatRoomService;
import iuh.fit.goat.service.MessageService;
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

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatCallJoinLeaveDeclineTest {

    @Mock private ChatCallSessionRepository sessionRepo;
    @Mock private ChatCallParticipantRepository participantRepo;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomService chatRoomService;
    @Mock private MessageService messageService;
    @Mock private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @InjectMocks private ChatCallServiceImpl callService;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new User(); account.setAccountId(1000L);
    }

    @Test
    void joinCall_shouldSaveNewParticipantWhenNotExists() throws Exception {
        ChatCallSession session = new ChatCallSession(); session.setCallSessionId(10L); session.setStatus(ChatCallSessionStatus.ACTIVE);
        ChatRoom sr = new ChatRoom(); sr.setRoomId(1L); session.setChatRoom(sr);
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(session));
        when(chatRoomRepository.existsByRoomIdAndDeletedAtIsNull(1L)).thenReturn(true);
        when(chatRoomService.isUserInChatRoom(1L, account.getAccountId())).thenReturn(true);
        when(participantRepo.findBySessionCallSessionIdAndAccountAccountIdAndDeletedAtIsNull(10L, account.getAccountId()))
                .thenReturn(Optional.empty());
        when(participantRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = callService.joinCall(account, 1L, 10L, new JoinChatCallRequest(true, ChatCallType.VOICE));
        assertThat(resp).isNotNull();
        verify(participantRepo).save(any(ChatCallParticipant.class));
    }

    @Test
    void leaveCall_shouldEndSessionWhenNoActiveParticipants() throws Exception {
        ChatCallSession session = new ChatCallSession(); session.setCallSessionId(20L); session.setStatus(ChatCallSessionStatus.ACTIVE);
        ChatRoom sr2 = new ChatRoom(); sr2.setRoomId(1L); session.setChatRoom(sr2);
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(session));
        when(chatRoomRepository.existsByRoomIdAndDeletedAtIsNull(1L)).thenReturn(true);
        ChatCallParticipant participant = new ChatCallParticipant(); participant.setParticipantId(5L); participant.setAccount(account);
        when(chatRoomService.isUserInChatRoom(1L, account.getAccountId())).thenReturn(true);
        when(participantRepo.findBySessionCallSessionIdAndAccountAccountIdAndDeletedAtIsNull(20L, account.getAccountId()))
                .thenReturn(Optional.of(participant));
        when(chatRoomService.isUserInChatRoom(1L, account.getAccountId())).thenReturn(true);
        when(participantRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(participantRepo.findBySessionCallSessionIdAndDeletedAtIsNull(20L)).thenReturn(List.of());
        when(sessionRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = callService.leaveCall(account, 1L, 20L);
        assertThat(resp).isNotNull();
        verify(messageService).createAndSendCallMessage(eq(1L), any(), any());
    }

    @Test
    void declineCall_group_shouldSaveDeclinedParticipant() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(2L); room.setType(ChatRoomType.GROUP);
        ChatCallSession session = new ChatCallSession(); session.setCallSessionId(30L); session.setChatRoom(room); session.setStatus(ChatCallSessionStatus.ACTIVE);
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(30L)).thenReturn(Optional.of(session));
        when(participantRepo.findBySessionCallSessionIdAndAccountAccountIdAndDeletedAtIsNull(30L, account.getAccountId()))
                .thenReturn(Optional.empty());
        when(chatRoomService.isUserInChatRoom(2L, account.getAccountId())).thenReturn(true);
        when(participantRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = callService.declineCall(account, 2L, 30L);
        assertThat(resp).isNotNull();
        verify(participantRepo).save(any(ChatCallParticipant.class));
    }
}
