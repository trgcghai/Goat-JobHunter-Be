package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.MessageEvent;
import iuh.fit.goat.dto.response.message.PinnedMessageResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.ChatRoomPermissionAction;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.service.MessageService;
import iuh.fit.goat.service.helper.ChatRoomPermissionGuard;
import iuh.fit.goat.service.helper.MessageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PinnedMessageServiceImplTest {

    @Mock private MessageService messageService;
    @Mock private MessageRepository messageRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private MessageHelper messageHelper;
    @Mock private ChatRoomPermissionGuard chatRoomPermissionGuard;

    @InjectMocks
    private PinnedMessageServiceImpl pinnedMessageService;

    private User currentUser;
    private ChatRoom chatRoom;
    private ChatMember member;
    private Message message;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setAccountId(700L);
        currentUser.setEmail("pin@example.com");
        currentUser.setFullName("Pin User");

        chatRoom = new ChatRoom();
        chatRoom.setRoomId(88L);

        member = new ChatMember();
        member.setAccount(currentUser);

        message = new Message();
        message.setChatRoomId("88");
        message.setMessageId("msg-pin");
        message.setContent("Hello pin");
    }

    @Test
    void pinMessage_shouldSavePinnedMessage() throws Exception {
        PinnedMessage pinnedMessage = new PinnedMessage();
        pinnedMessage.setChatRoomId("88");
        pinnedMessage.setMessageId("msg-pin");
        pinnedMessage.setPinnedBy("Pin User");

        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);
        when(messageRepository.getPinnedMessagesByChatRoom("88")).thenReturn(List.of());
        when(messageRepository.findByChatRoomIdAndMessageId("88", "msg-pin")).thenReturn(Optional.of(message));
        when(messageRepository.isPinned("88", "msg-pin")).thenReturn(false);
        when(accountRepository.findByEmailAndDeletedAtIsNull("pin@example.com")).thenReturn(Optional.of(currentUser));
        when(messageRepository.pinMessage("88", "msg-pin", "Pin User")).thenReturn(pinnedMessage);

        PinnedMessageResponse response = pinnedMessageService.pinMessage(88L, "msg-pin", currentUser);

        assertThat(response.getMessageId()).isEqualTo("msg-pin");
        assertThat(response.getPinnedBy()).isEqualTo("Pin User");
        verify(messageService).createAndSendSystemMessage(eq(88L), eq(MessageEvent.MESSAGE_PINNED), eq(currentUser), any(), eq("msg-pin"));
    }

    @Test
    void unpinMessage_shouldDeletePinnedMessage() throws Exception {
        PinnedMessage pinnedMessage = new PinnedMessage();
        pinnedMessage.setChatRoomId("88");
        pinnedMessage.setMessageId("msg-pin");
        pinnedMessage.setPinnedBy("Pin User");

        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);
        when(messageRepository.getPinnedMessage("88", "msg-pin")).thenReturn(pinnedMessage);
        when(accountRepository.findByEmailAndDeletedAtIsNull("pin@example.com")).thenReturn(Optional.of(currentUser));
        when(messageRepository.findByChatRoomIdAndMessageId("88", "msg-pin")).thenReturn(Optional.of(message));

        pinnedMessageService.unpinMessage(88L, "msg-pin", currentUser);

        verify(messageRepository).deletePinnedMessage("88", "msg-pin");
        verify(messageService).createAndSendSystemMessage(eq(88L), eq(MessageEvent.MESSAGE_UNPINNED), eq(currentUser), any(), eq("msg-pin"));
    }

    @Test
    void isMessagePinned_shouldReturnRepositoryValue() {
        when(messageRepository.isPinned("88", "msg-pin")).thenReturn(true);

        assertThat(pinnedMessageService.isMessagePinned(88L, "msg-pin")).isTrue();
    }
}