package iuh.fit.goat.service.impl;

import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.MessageType;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.service.AiService;
import iuh.fit.goat.service.StorageService;
import iuh.fit.goat.service.cache.ChatRoomCacheService;
import iuh.fit.goat.service.helper.ChatRoomPermissionGuard;
import iuh.fit.goat.service.helper.MessageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock private StorageService storageService;
    @Mock private AiService aiService;
    @Mock private MessageHiddenRepository messageHiddenRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChatRoomCacheService chatRoomCache;
    @Mock private MessageHelper messageHelper;
    @Mock private ChatRoomPermissionGuard chatRoomPermissionGuard;

    @InjectMocks
    private MessageServiceImpl messageService;

    private User currentUser;
    private ChatRoom chatRoom;
    private ChatMember member;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setAccountId(601L);
        currentUser.setEmail("sender@example.com");
        currentUser.setUsername("sender");
        currentUser.setFullName("Sender One");

        chatRoom = new ChatRoom();
        chatRoom.setRoomId(77L);

        member = new ChatMember();
        member.setAccount(currentUser);
    }

    @Test
    void translateMessage_shouldCallAiService() throws Exception {
        when(messageHelper.normalizeMessageContent("Hello world")).thenReturn("Hello world");
        when(aiService.translateText("Hello world", "vi")).thenReturn("Xin chao the gioi");

        var response = messageService.translateMessage("Hello world", "vi");

        assertThat(response.getSourceText()).isEqualTo("Hello world");
        assertThat(response.getTranslatedText()).isEqualTo("Xin chao the gioi");
        verify(aiService).translateText("Hello world", "vi");
    }

    @Test
    void hideMessageForMe_shouldRecordHiddenMessage() throws Exception {
        Message message = new Message();
        message.setChatRoomId("77");
        message.setMessageId("m1");
        message.setIsHidden(false);

        when(messageHelper.isUserInChatRoom(77L, 601L)).thenReturn(true);
        when(messageRepository.findByChatRoomIdAndMessageId("77", "m1")).thenReturn(Optional.of(message));

        messageService.hideMessageForMe(77L, "m1", currentUser);

        verify(messageHiddenRepository).hideMessageForUser(eq("m1"), eq(601L), any(Instant.class));
    }
}