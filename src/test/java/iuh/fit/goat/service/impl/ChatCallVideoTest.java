package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.chat.EndChatCallRequest;
import iuh.fit.goat.dto.request.chat.JoinChatCallRequest;
import iuh.fit.goat.dto.request.chat.StartChatCallRequest;
import iuh.fit.goat.dto.response.chat.ChatRoomResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.ChatCallParticipant;
import iuh.fit.goat.entity.ChatCallSession;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.User;
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

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatCallVideoTest {

    @Mock private ChatCallSessionRepository sessionRepo;
    @Mock private ChatCallParticipantRepository participantRepo;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomService chatRoomService;
    @Mock private MessageService messageService;
    @Mock private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @InjectMocks private ChatCallServiceImpl callService;

    private Account currentAccount;

    @BeforeEach
    void setUp() {
        currentAccount = new User();
        currentAccount.setAccountId(900L);
    }

    @Test
    void startCall_directVideo_success() throws Exception {
        Long roomId = 101L;
        ChatRoom room = chatRoom(ChatRoomType.DIRECT, roomId);
        ChatRoomResponse roomResponse = roomResponse(roomId, ChatRoomType.DIRECT, "Direct Room");
        ChatCallSession savedSession = new ChatCallSession();
        savedSession.setCallSessionId(201L);
        savedSession.setChatRoom(room);
        savedSession.setStatus(ChatCallSessionStatus.ACTIVE);
        savedSession.setCallType(ChatCallType.VIDEO);

        when(chatRoomService.isUserInChatRoom(roomId, currentAccount.getAccountId())).thenReturn(true);
        when(sessionRepo.existsByChatRoomRoomIdAndStatusInAndDeletedAtIsNull(eq(roomId), any(EnumSet.class))).thenReturn(false);
        when(chatRoomRepository.findByRoomId(roomId)).thenReturn(Optional.of(room));
        when(sessionRepo.save(any(ChatCallSession.class))).thenAnswer(invocation -> {
            ChatCallSession session = invocation.getArgument(0);
            session.setCallSessionId(savedSession.getCallSessionId());
            return session;
        });
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(201L)).thenReturn(Optional.of(savedSession));
        when(participantRepo.save(any(ChatCallParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepo.findBySessionCallSessionIdAndDeletedAtIsNull(201L)).thenReturn(List.of());
        when(chatRoomService.getDetailChatRoomInformation(currentAccount, roomId)).thenReturn(roomResponse);

        var response = callService.startCall(currentAccount, roomId, new StartChatCallRequest(true, ChatCallType.VIDEO));

        assertThat(response.getCallType()).isEqualTo(ChatCallType.VIDEO);
        assertThat(response.getChatRoomId()).isEqualTo(roomId);
        verify(participantRepo).save(any(ChatCallParticipant.class));
    }

    @Test
    void startCall_groupVideo_success() throws Exception {
        Long roomId = 102L;
        ChatRoom room = chatRoom(ChatRoomType.GROUP, roomId);
        ChatRoomResponse roomResponse = roomResponse(roomId, ChatRoomType.GROUP, "Group Room");
        ChatCallSession savedSession = new ChatCallSession();
        savedSession.setCallSessionId(202L);
        savedSession.setChatRoom(room);
        savedSession.setStatus(ChatCallSessionStatus.ACTIVE);
        savedSession.setCallType(ChatCallType.VIDEO);

        when(chatRoomService.isUserInChatRoom(roomId, currentAccount.getAccountId())).thenReturn(true);
        when(sessionRepo.existsByChatRoomRoomIdAndStatusInAndDeletedAtIsNull(eq(roomId), any(EnumSet.class))).thenReturn(false);
        when(chatRoomRepository.findByRoomId(roomId)).thenReturn(Optional.of(room));
        when(sessionRepo.save(any(ChatCallSession.class))).thenAnswer(invocation -> {
            ChatCallSession session = invocation.getArgument(0);
            session.setCallSessionId(savedSession.getCallSessionId());
            return session;
        });
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(202L)).thenReturn(Optional.of(savedSession));
        when(participantRepo.save(any(ChatCallParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepo.findBySessionCallSessionIdAndDeletedAtIsNull(202L)).thenReturn(List.of());
        when(chatRoomService.getDetailChatRoomInformation(currentAccount, roomId)).thenReturn(roomResponse);

        var response = callService.startCall(currentAccount, roomId, new StartChatCallRequest(false, ChatCallType.VIDEO));

        assertThat(response.getCallType()).isEqualTo(ChatCallType.VIDEO);
        assertThat(response.getChatRoomId()).isEqualTo(roomId);
        verify(participantRepo).save(any(ChatCallParticipant.class));
    }

    @Test
    void startCall_videoWhenActiveCallExists_throwsInvalid() throws Exception {
        Long roomId = 103L;
        when(chatRoomService.isUserInChatRoom(roomId, currentAccount.getAccountId())).thenReturn(true);
        when(sessionRepo.existsByChatRoomRoomIdAndStatusInAndDeletedAtIsNull(eq(roomId), any(EnumSet.class))).thenReturn(true);

        assertThrows(InvalidException.class, () -> callService.startCall(currentAccount, roomId, new StartChatCallRequest(true, ChatCallType.VIDEO)));
    }

    @Test
    void startCall_videoNonMember_throwsPermission() throws Exception {
        Long roomId = 104L;
        when(chatRoomService.isUserInChatRoom(roomId, currentAccount.getAccountId())).thenReturn(false);

        assertThrows(PermissionException.class, () -> callService.startCall(currentAccount, roomId, new StartChatCallRequest(true, ChatCallType.VIDEO)));
    }

    @Test
    void joinCall_videoDirect_success() throws Exception {
        Long roomId = 201L;
        Long sessionId = 301L;
        ChatRoom room = chatRoom(ChatRoomType.DIRECT, roomId);
        ChatRoomResponse roomResponse = roomResponse(roomId, ChatRoomType.DIRECT, "Direct Room");
        ChatCallSession session = new ChatCallSession();
        session.setCallSessionId(sessionId);
        session.setChatRoom(room);
        session.setStatus(ChatCallSessionStatus.ACTIVE);
        session.setCallType(ChatCallType.VIDEO);

        when(chatRoomService.isUserInChatRoom(roomId, currentAccount.getAccountId())).thenReturn(true);
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(Optional.of(session));
        when(participantRepo.findBySessionCallSessionIdAndAccountAccountIdAndDeletedAtIsNull(sessionId, currentAccount.getAccountId()))
                .thenReturn(Optional.empty());
        when(participantRepo.save(any(ChatCallParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(Optional.of(session));
        when(participantRepo.findBySessionCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(List.of());
        when(chatRoomService.getDetailChatRoomInformation(currentAccount, roomId)).thenReturn(roomResponse);

        var response = callService.joinCall(currentAccount, roomId, sessionId, new JoinChatCallRequest(true, ChatCallType.VIDEO));

        assertThat(response.getCallType()).isEqualTo(ChatCallType.VIDEO);
        verify(participantRepo).save(any(ChatCallParticipant.class));
    }

    @Test
    void joinCall_videoGroup_typeMismatch_throwsInvalid() throws Exception {
        Long roomId = 202L;
        Long sessionId = 302L;
        ChatRoom room = chatRoom(ChatRoomType.GROUP, roomId);
        ChatCallSession session = new ChatCallSession();
        session.setCallSessionId(sessionId);
        session.setChatRoom(room);
        session.setStatus(ChatCallSessionStatus.ACTIVE);
        session.setCallType(ChatCallType.VIDEO);

        when(chatRoomService.isUserInChatRoom(roomId, currentAccount.getAccountId())).thenReturn(true);
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(Optional.of(session));

        assertThrows(InvalidException.class, () -> callService.joinCall(currentAccount, roomId, sessionId, new JoinChatCallRequest(true, ChatCallType.VOICE)));
    }

    @Test
    void leaveCall_videoGroupWithRemainingParticipants_returnsLeaveEvent() throws Exception {
        Long roomId = 203L;
        Long sessionId = 303L;
        ChatRoom room = chatRoom(ChatRoomType.GROUP, roomId);
        ChatRoomResponse roomResponse = roomResponse(roomId, ChatRoomType.GROUP, "Group Room");
        ChatCallSession session = new ChatCallSession();
        session.setCallSessionId(sessionId);
        session.setChatRoom(room);
        session.setStatus(ChatCallSessionStatus.ACTIVE);
        session.setCallType(ChatCallType.VIDEO);
        ChatCallParticipant leaving = participant(currentAccount, sessionId);
        ChatCallParticipant other = participant(new User(), sessionId);

        when(chatRoomService.isUserInChatRoom(roomId, currentAccount.getAccountId())).thenReturn(true);
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(Optional.of(session));
        when(participantRepo.findBySessionCallSessionIdAndAccountAccountIdAndDeletedAtIsNull(sessionId, currentAccount.getAccountId()))
                .thenReturn(Optional.of(leaving));
        when(participantRepo.save(any(ChatCallParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepo.findBySessionCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(List.of(leaving, other));
        when(chatRoomService.getDetailChatRoomInformation(currentAccount, roomId)).thenReturn(roomResponse);

        var response = callService.leaveCall(currentAccount, roomId, sessionId);

        assertThat(response.getCallType()).isEqualTo(ChatCallType.VIDEO);
        verify(sessionRepo, never()).save(any(ChatCallSession.class));
    }

    @Test
    void leaveCall_videoLastParticipant_endsSession() throws Exception {
        Long roomId = 204L;
        Long sessionId = 304L;
        ChatRoom room = chatRoom(ChatRoomType.DIRECT, roomId);
        ChatRoomResponse roomResponse = roomResponse(roomId, ChatRoomType.DIRECT, "Direct Room");
        ChatCallSession session = new ChatCallSession();
        session.setCallSessionId(sessionId);
        session.setChatRoom(room);
        session.setStatus(ChatCallSessionStatus.ACTIVE);
        session.setCallType(ChatCallType.VIDEO);
        ChatCallParticipant leaving = participant(currentAccount, sessionId);

        when(chatRoomService.isUserInChatRoom(roomId, currentAccount.getAccountId())).thenReturn(true);
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(Optional.of(session));
        when(participantRepo.findBySessionCallSessionIdAndAccountAccountIdAndDeletedAtIsNull(sessionId, currentAccount.getAccountId()))
                .thenReturn(Optional.of(leaving));
        when(participantRepo.save(any(ChatCallParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepo.findBySessionCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(List.of(leaving));
        when(sessionRepo.save(any(ChatCallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepo.findBySessionCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(List.of(leaving));
        when(chatRoomService.getDetailChatRoomInformation(currentAccount, roomId)).thenReturn(roomResponse);

        var response = callService.leaveCall(currentAccount, roomId, sessionId);

        assertThat(response.getStatus()).isEqualTo(ChatCallSessionStatus.ENDED);
        verify(sessionRepo).save(any(ChatCallSession.class));
        verify(messageService).createAndSendCallMessage(eq(roomId), any(), any());
    }

    @Test
    void endCall_groupVideoByInitiator_success() throws Exception {
        Long roomId = 205L;
        Long sessionId = 305L;
        ChatRoom room = chatRoom(ChatRoomType.GROUP, roomId);
        ChatRoomResponse roomResponse = roomResponse(roomId, ChatRoomType.GROUP, "Group Room");
        ChatCallSession session = new ChatCallSession();
        session.setCallSessionId(sessionId);
        session.setChatRoom(room);
        session.setStatus(ChatCallSessionStatus.ACTIVE);
        session.setCallType(ChatCallType.VIDEO);
        User initiator = new User();
        initiator.setAccountId(currentAccount.getAccountId());
        session.setInitiator(initiator);
        ChatCallParticipant participant = participant(currentAccount, sessionId);

        when(chatRoomService.isUserInChatRoom(roomId, currentAccount.getAccountId())).thenReturn(true);
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(Optional.of(session));
        when(participantRepo.findBySessionCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(List.of(participant));
        when(participantRepo.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepo.save(any(ChatCallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomService.getDetailChatRoomInformation(currentAccount, roomId)).thenReturn(roomResponse);

        var response = callService.endCall(currentAccount, roomId, sessionId, new EndChatCallRequest(ChatCallEndReason.HANGUP));

        assertThat(response.getStatus()).isEqualTo(ChatCallSessionStatus.ENDED);
        assertThat(response.getCallType()).isEqualTo(ChatCallType.VIDEO);
        verify(messageService).createAndSendCallMessage(eq(roomId), any(), any());
    }

    @Test
    void endCall_groupVideoByNonInitiator_throwsPermission() throws Exception {
        Long roomId = 206L;
        Long sessionId = 306L;
        ChatRoom room = chatRoom(ChatRoomType.GROUP, roomId);
        ChatCallSession session = new ChatCallSession();
        session.setCallSessionId(sessionId);
        session.setChatRoom(room);
        session.setStatus(ChatCallSessionStatus.ACTIVE);
        session.setCallType(ChatCallType.VIDEO);
        User initiator = new User();
        initiator.setAccountId(9999L);
        session.setInitiator(initiator);

        when(chatRoomService.isUserInChatRoom(roomId, currentAccount.getAccountId())).thenReturn(true);
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(Optional.of(session));

        assertThrows(PermissionException.class, () -> callService.endCall(currentAccount, roomId, sessionId, new EndChatCallRequest(ChatCallEndReason.HANGUP)));
    }

    @Test
    void declineCall_directVideoDelegatesToEndCall() throws Exception {
        Long roomId = 207L;
        Long sessionId = 307L;
        ChatRoom room = chatRoom(ChatRoomType.DIRECT, roomId);
        ChatRoomResponse roomResponse = roomResponse(roomId, ChatRoomType.DIRECT, "Direct Room");
        ChatCallSession session = new ChatCallSession();
        session.setCallSessionId(sessionId);
        session.setChatRoom(room);
        session.setStatus(ChatCallSessionStatus.ACTIVE);
        session.setCallType(ChatCallType.VIDEO);

        when(chatRoomService.isUserInChatRoom(roomId, currentAccount.getAccountId())).thenReturn(true);
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(Optional.of(session));
        when(participantRepo.findBySessionCallSessionIdAndAccountAccountIdAndDeletedAtIsNull(sessionId, currentAccount.getAccountId()))
                .thenReturn(Optional.empty());
        when(participantRepo.findBySessionCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(List.of());
        when(participantRepo.save(any(ChatCallParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepo.save(any(ChatCallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomService.getDetailChatRoomInformation(currentAccount, roomId)).thenReturn(roomResponse);

        var response = callService.declineCall(currentAccount, roomId, sessionId);

        assertThat(response.getStatus()).isEqualTo(ChatCallSessionStatus.ENDED);
        verify(sessionRepo).save(any(ChatCallSession.class));
    }

    @Test
    void declineCall_groupVideo_marksParticipantDeclined() throws Exception {
        Long roomId = 208L;
        Long sessionId = 308L;
        ChatRoom room = chatRoom(ChatRoomType.GROUP, roomId);
        ChatRoomResponse roomResponse = roomResponse(roomId, ChatRoomType.GROUP, "Group Room");
        ChatCallSession session = new ChatCallSession();
        session.setCallSessionId(sessionId);
        session.setChatRoom(room);
        session.setStatus(ChatCallSessionStatus.ACTIVE);
        session.setCallType(ChatCallType.VIDEO);
        ChatCallParticipant participant = participant(currentAccount, sessionId);

        when(chatRoomService.isUserInChatRoom(roomId, currentAccount.getAccountId())).thenReturn(true);
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(Optional.of(session));
        when(participantRepo.findBySessionCallSessionIdAndAccountAccountIdAndDeletedAtIsNull(sessionId, currentAccount.getAccountId()))
                .thenReturn(Optional.of(participant));
        when(participantRepo.save(any(ChatCallParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepo.findBySessionCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(List.of(participant));
        when(chatRoomService.getDetailChatRoomInformation(currentAccount, roomId)).thenReturn(roomResponse);

        var response = callService.declineCall(currentAccount, roomId, sessionId);

        assertThat(response.getCallType()).isEqualTo(ChatCallType.VIDEO);
        verify(participantRepo).save(any(ChatCallParticipant.class));
    }

    @Test
    void getCurrentCall_videoReturnsActiveSession() throws Exception {
        Long roomId = 209L;
        Long sessionId = 309L;
        ChatRoom room = chatRoom(ChatRoomType.DIRECT, roomId);
        ChatRoomResponse roomResponse = roomResponse(roomId, ChatRoomType.DIRECT, "Direct Room");
        ChatCallSession session = new ChatCallSession();
        session.setCallSessionId(sessionId);
        session.setChatRoom(room);
        session.setStatus(ChatCallSessionStatus.ACTIVE);
        session.setCallType(ChatCallType.VIDEO);

        when(chatRoomService.isUserInChatRoom(roomId, currentAccount.getAccountId())).thenReturn(true);
        when(sessionRepo.findFirstByChatRoomRoomIdAndStatusInAndDeletedAtIsNullOrderByCreatedAtDesc(eq(roomId), any(EnumSet.class)))
                .thenReturn(Optional.of(session));
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(Optional.of(session));
        when(participantRepo.findBySessionCallSessionIdAndDeletedAtIsNull(sessionId)).thenReturn(List.of());
        when(chatRoomService.getDetailChatRoomInformation(currentAccount, roomId)).thenReturn(roomResponse);

        var response = callService.getCurrentCall(currentAccount, roomId);

        assertThat(response.getCallType()).isEqualTo(ChatCallType.VIDEO);
        assertThat(response.getStatus()).isEqualTo(ChatCallSessionStatus.ACTIVE);
    }

    private ChatRoom chatRoom(ChatRoomType type, Long roomId) {
        ChatRoom room = new ChatRoom();
        room.setRoomId(roomId);
        room.setType(type);
        return room;
    }

    private ChatRoomResponse roomResponse(Long roomId, ChatRoomType type, String name) {
        return ChatRoomResponse.builder()
                .roomId(roomId)
                .type(type)
                .name(name)
                .avatar("avatar.png")
                .build();
    }

    private ChatCallParticipant participant(Account account, Long sessionId) {
        ChatCallSession session = new ChatCallSession();
        session.setCallSessionId(sessionId);
        ChatCallParticipant participant = new ChatCallParticipant();
        participant.setSession(session);
        participant.setAccount(account);
        participant.setJoinedAt(java.time.Instant.now());
        participant.setPublisher(true);
        return participant;
    }
}