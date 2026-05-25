package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.chat.EndChatCallRequest;
import iuh.fit.goat.dto.request.chat.JoinChatCallRequest;
import iuh.fit.goat.dto.request.chat.StartChatCallRequest;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.ChatCallEndReason;
import iuh.fit.goat.enumeration.ChatCallSessionStatus;
import iuh.fit.goat.enumeration.ChatCallType;
import iuh.fit.goat.enumeration.ChatRoomType;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.PermissionException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatCallVoiceTest {

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
        account = new User(); account.setAccountId(123L);
    }

    @Test
    void startCall_success_direct_createsSessionAndPublishes() throws Exception {
        Long roomId = 11L;
        ChatRoom room = new ChatRoom(); room.setRoomId(roomId); room.setType(ChatRoomType.DIRECT);
        iuh.fit.goat.dto.response.chat.ChatRoomResponse roomResponse = iuh.fit.goat.dto.response.chat.ChatRoomResponse.builder()
            .roomId(roomId)
            .type(ChatRoomType.DIRECT)
            .name("Direct Room")
            .avatar("avatar.png")
            .build();
        ChatCallSession savedSession = new ChatCallSession();
        savedSession.setCallSessionId(1L);
        savedSession.setChatRoom(room);
        savedSession.setStatus(ChatCallSessionStatus.ACTIVE);
        savedSession.setCallType(ChatCallType.VOICE);
        when(chatRoomRepository.findByRoomId(roomId)).thenReturn(Optional.of(room));
        when(chatRoomService.isUserInChatRoom(roomId, account.getAccountId())).thenReturn(true);
        when(sessionRepo.existsByChatRoomRoomIdAndStatusInAndDeletedAtIsNull(eq(roomId), any(java.util.EnumSet.class))).thenReturn(false);
        when(sessionRepo.save(any())).thenAnswer(i -> { ChatCallSession s = i.getArgument(0); s.setCallSessionId(savedSession.getCallSessionId()); return s; });
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(savedSession));
        when(participantRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(participantRepo.findBySessionCallSessionIdAndDeletedAtIsNull(1L)).thenReturn(List.of());
        when(chatRoomService.getDetailChatRoomInformation(account, roomId)).thenReturn(roomResponse);

        var resp = callService.startCall(account, roomId, new StartChatCallRequest(true, ChatCallType.VOICE));
        assertThat(resp).isNotNull();
        verify(participantRepo).save(any());
        verify(messagingTemplate).convertAndSend(anyString(), any(iuh.fit.goat.dto.response.chat.ChatCallRealtimeEventResponse.class));
    }

    @Test
    void startCall_whenActiveCallExists_throwsInvalid() throws Exception {
        Long roomId = 12L;
        when(chatRoomService.isUserInChatRoom(roomId, account.getAccountId())).thenReturn(true);
        when(sessionRepo.existsByChatRoomRoomIdAndStatusInAndDeletedAtIsNull(eq(roomId), any(java.util.EnumSet.class))).thenReturn(true);

        assertThrows(InvalidException.class, () -> callService.startCall(account, roomId, null));
    }

    @Test
    void startCall_nonMember_throwsPermission() throws Exception {
        Long roomId = 13L;
        when(chatRoomService.isUserInChatRoom(roomId, account.getAccountId())).thenReturn(false);

        assertThrows(PermissionException.class, () -> callService.startCall(account, roomId, null));
    }

    @Test
    void joinCall_requestedTypeMismatch_throwsInvalid() throws Exception {
        Long sessionId = 21L;
        ChatCallSession session = new ChatCallSession(); session.setCallSessionId(sessionId); session.setStatus(ChatCallSessionStatus.ACTIVE);
        session.setCallType(ChatCallType.VOICE);
        ChatRoom room = new ChatRoom(); room.setRoomId(2L); session.setChatRoom(room);

        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(Optional.of(session));
        when(chatRoomService.isUserInChatRoom(2L, account.getAccountId())).thenReturn(true);

        assertThrows(InvalidException.class, () -> callService.joinCall(account, 2L, sessionId, new JoinChatCallRequest(true, ChatCallType.VIDEO)));
    }

    @Test
    void joinCall_existingParticipant_updatesJoinedAt() throws Exception {
        Long sessionId = 22L;
        ChatCallSession session = new ChatCallSession(); session.setCallSessionId(sessionId); session.setStatus(ChatCallSessionStatus.ACTIVE);
        ChatRoom room = new ChatRoom(); room.setRoomId(3L); session.setChatRoom(room);
        ChatCallParticipant existing = new ChatCallParticipant(); existing.setParticipantId(7L); existing.setAccount(account);

        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(Optional.of(session));
        when(chatRoomService.isUserInChatRoom(3L, account.getAccountId())).thenReturn(true);
        when(participantRepo.findBySessionCallSessionIdAndAccountAccountIdAndDeletedAtIsNull(sessionId, account.getAccountId()))
                .thenReturn(Optional.of(existing));
        when(participantRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = callService.joinCall(account, 3L, sessionId, new JoinChatCallRequest(false, ChatCallType.VOICE));
        assertThat(resp).isNotNull();
        verify(participantRepo).save(existing);
    }

    @Test
    void leaveCall_withRemainingParticipants_publishesLeaveNotEnd() throws Exception {
        Long sessionId = 31L; Long roomId = 4L;
        ChatCallSession session = new ChatCallSession(); session.setCallSessionId(sessionId); session.setStatus(ChatCallSessionStatus.ACTIVE);
        ChatRoom room = new ChatRoom(); room.setRoomId(roomId); session.setChatRoom(room);
        ChatCallParticipant leaving = new ChatCallParticipant(); leaving.setParticipantId(9L); leaving.setAccount(account);
        ChatCallParticipant other = new ChatCallParticipant(); other.setParticipantId(10L); other.setAccount(new User()); other.setLeftAt(null);

        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(Optional.of(session));
        when(chatRoomService.isUserInChatRoom(roomId, account.getAccountId())).thenReturn(true);
        when(participantRepo.findBySessionCallSessionIdAndAccountAccountIdAndDeletedAtIsNull(sessionId, account.getAccountId()))
                .thenReturn(Optional.of(leaving));
        when(participantRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(participantRepo.findBySessionCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(List.of(leaving, other));

        var resp = callService.leaveCall(account, roomId, sessionId);
        assertThat(resp).isNotNull();
        verify(messagingTemplate).convertAndSend(anyString(), any(iuh.fit.goat.dto.response.chat.ChatCallRealtimeEventResponse.class));
        verify(sessionRepo, never()).save(any());
    }

    @Test
    void endCall_group_byInitiator_setsEndedAndSendsMessage() throws Exception {
        Long sessionId = 41L; Long roomId = 5L;
        User initiator = new User(); initiator.setAccountId(account.getAccountId());
        ChatCallSession session = new ChatCallSession(); session.setCallSessionId(sessionId); session.setStatus(ChatCallSessionStatus.ACTIVE); session.setInitiator(initiator);
        ChatRoom room = new ChatRoom(); room.setRoomId(roomId); room.setType(ChatRoomType.GROUP); session.setChatRoom(room);
        ChatCallParticipant p1 = new ChatCallParticipant(); p1.setAccount(account); p1.setLeftAt(null);

        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(Optional.of(session));
        when(chatRoomService.isUserInChatRoom(roomId, account.getAccountId())).thenReturn(true);
        when(participantRepo.findBySessionCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(List.of(p1));
        when(participantRepo.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(sessionRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = callService.endCall(account, roomId, sessionId, new EndChatCallRequest(ChatCallEndReason.HANGUP));
        assertThat(resp).isNotNull();
        verify(messageService).createAndSendCallMessage(eq(roomId), any(), any());
        verify(messagingTemplate).convertAndSend(anyString(), any(iuh.fit.goat.dto.response.chat.ChatCallRealtimeEventResponse.class));
    }

    @Test
    void endCall_group_byNonInitiator_throwsPermission() throws Exception {
        Long sessionId = 42L; Long roomId = 6L;
        User initiator = new User(); initiator.setAccountId(999L);
        ChatCallSession session = new ChatCallSession(); session.setCallSessionId(sessionId); session.setStatus(ChatCallSessionStatus.ACTIVE); session.setInitiator(initiator);
        ChatRoom room = new ChatRoom(); room.setRoomId(roomId); room.setType(ChatRoomType.GROUP); session.setChatRoom(room);

        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(Optional.of(session));
        when(chatRoomService.isUserInChatRoom(roomId, account.getAccountId())).thenReturn(true);

        assertThrows(PermissionException.class, () -> callService.endCall(account, roomId, sessionId, new EndChatCallRequest(null)));
    }

    @Test
    void getCurrentCall_noActiveCall_throwsInvalid() throws Exception {
        Long roomId = 7L;
        when(chatRoomService.isUserInChatRoom(roomId, account.getAccountId())).thenReturn(true);
        when(sessionRepo.findFirstByChatRoomRoomIdAndStatusInAndDeletedAtIsNullOrderByCreatedAtDesc(eq(roomId), any(java.util.EnumSet.class))).thenReturn(Optional.empty());

        assertThrows(InvalidException.class, () -> callService.getCurrentCall(account, roomId));
    }

    @Test
    void declineCall_direct_shouldEndCall() throws Exception {
        Long sessionId = 51L; Long roomId = 8L;
        ChatRoom room = new ChatRoom(); room.setRoomId(roomId); room.setType(ChatRoomType.DIRECT);
        ChatCallSession session = new ChatCallSession(); session.setCallSessionId(sessionId); session.setStatus(ChatCallSessionStatus.ACTIVE); session.setChatRoom(room);

        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(Optional.of(session));
        when(chatRoomService.isUserInChatRoom(roomId, account.getAccountId())).thenReturn(true);
        when(participantRepo.findBySessionCallSessionIdAndAccountAccountIdAndDeletedAtIsNull(sessionId, account.getAccountId()))
                .thenReturn(Optional.empty());
        when(participantRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(participantRepo.findBySessionCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(List.of());
        when(sessionRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = callService.declineCall(account, roomId, sessionId);
        assertThat(resp).isNotNull();
        verify(sessionRepo).save(any());
    }
}
