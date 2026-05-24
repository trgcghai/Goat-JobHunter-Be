package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.chat.StartChatCallRequest;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.ChatCallSession;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.ChatCallType;
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

import java.util.Optional;
import iuh.fit.goat.entity.ChatRoom;

import iuh.fit.goat.enumeration.ChatCallSessionStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatCallServiceImplTest {

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
        account = new User(); account.setAccountId(900L);
    }

    @Test
    void startCall_shouldCreateSession() throws Exception {
        when(chatRoomService.isUserInChatRoom(1L, 900L)).thenReturn(true);
        when(chatRoomRepository.findByRoomId(1L)).thenReturn(Optional.of(new ChatRoom()));
        StartChatCallRequest req = new StartChatCallRequest(true, ChatCallType.VOICE);
        when(sessionRepo.existsByChatRoomRoomIdAndStatusInAndDeletedAtIsNull(eq(1L), any())).thenReturn(false);
        when(sessionRepo.save(any(ChatCallSession.class))).thenAnswer(i -> {
            ChatCallSession s = i.getArgument(0);
            s.setCallSessionId(77L);
            return s;
        });

        ChatCallSession saved = new ChatCallSession();
        saved.setCallSessionId(77L);
        ChatRoom room = new ChatRoom(); room.setRoomId(1L);
        saved.setChatRoom(room);
        saved.setStatus(ChatCallSessionStatus.ACTIVE);
        saved.setCallType(ChatCallType.VOICE);
        saved.setInitiator(account);
        when(sessionRepo.findByCallSessionIdAndDeletedAtIsNull(anyLong())).thenReturn(Optional.of(saved));

        var resp = callService.startCall(account, 1L, req);
        assertThat(resp).isNotNull();
        verify(sessionRepo).save(any(ChatCallSession.class));
    }

}
