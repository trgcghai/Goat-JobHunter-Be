package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.MessageEvent;
import iuh.fit.goat.dto.response.message.PinnedMessageResponse;
import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.PinnedMessage;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.ChatRoomPermissionAction;
import iuh.fit.goat.exception.InvalidException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
    private PinnedMessage pinnedMessage;

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

        pinnedMessage = new PinnedMessage();
        pinnedMessage.setChatRoomId("88");
        pinnedMessage.setMessageId("msg-pin");
        pinnedMessage.setPinnedBy("Pin User");
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
    void pinMessage_companyAccount_pinnedByCompanyName() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);
        when(messageRepository.getPinnedMessagesByChatRoom("88")).thenReturn(List.of());
        when(messageRepository.findByChatRoomIdAndMessageId("88", "msg-pin")).thenReturn(Optional.of(message));
        when(messageRepository.isPinned("88", "msg-pin")).thenReturn(false);

        Company company = new Company();
        company.setName("GoatCo");
        when(accountRepository.findByEmailAndDeletedAtIsNull("pin@example.com")).thenReturn(Optional.of(company));

        PinnedMessage pinnedMessage = new PinnedMessage();
        pinnedMessage.setChatRoomId("88");
        pinnedMessage.setMessageId("msg-pin");
        pinnedMessage.setPinnedBy("GoatCo");
        when(messageRepository.pinMessage("88", "msg-pin", "GoatCo")).thenReturn(pinnedMessage);

        PinnedMessageResponse response = pinnedMessageService.pinMessage(88L, "msg-pin", currentUser);

        assertThat(response.getPinnedBy()).isEqualTo("GoatCo");
    }

    @Test
    void pinMessage_chatRoomNotFound_throwsInvalid() {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.empty());

        assertThrows(InvalidException.class, () -> pinnedMessageService.pinMessage(88L, "msg-pin", currentUser));
        verifyNoInteractions(messageRepository, messageService, accountRepository, chatRoomPermissionGuard, messageHelper);
    }

    @Test
    void pinMessage_permissionDenied_throwsInvalid() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doThrow(new InvalidException("forbidden")).when(chatRoomPermissionGuard)
                .assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);

        assertThrows(InvalidException.class, () -> pinnedMessageService.pinMessage(88L, "msg-pin", currentUser));
        verify(messageRepository, never()).pinMessage(any(), any(), any());
        verifyNoInteractions(messageService);
    }

    @Test
    void pinMessage_exceedsLimit_throwsInvalid() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);
        when(messageRepository.getPinnedMessagesByChatRoom("88")).thenReturn(List.of(
                new PinnedMessage(), new PinnedMessage(), new PinnedMessage(), new PinnedMessage(), new PinnedMessage()));

        assertThrows(InvalidException.class, () -> pinnedMessageService.pinMessage(88L, "msg-pin", currentUser));
        verify(messageRepository, never()).findByChatRoomIdAndMessageId(any(), any());
        verifyNoInteractions(messageService);
    }

    @Test
    void pinMessage_messageNotFound_throwsInvalid() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);
        when(messageRepository.getPinnedMessagesByChatRoom("88")).thenReturn(List.of());
        when(messageRepository.findByChatRoomIdAndMessageId("88", "msg-pin")).thenReturn(Optional.empty());

        assertThrows(InvalidException.class, () -> pinnedMessageService.pinMessage(88L, "msg-pin", currentUser));
        verify(messageRepository, never()).pinMessage(any(), any(), any());
        verifyNoInteractions(messageService);
    }

    @Test
    void pinMessage_alreadyPinned_throwsInvalid() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);
        when(messageRepository.getPinnedMessagesByChatRoom("88")).thenReturn(List.of());
        when(messageRepository.findByChatRoomIdAndMessageId("88", "msg-pin")).thenReturn(Optional.of(message));
        when(messageRepository.isPinned("88", "msg-pin")).thenReturn(true);

        assertThrows(InvalidException.class, () -> pinnedMessageService.pinMessage(88L, "msg-pin", currentUser));
        verify(messageRepository, never()).pinMessage(any(), any(), any());
        verifyNoInteractions(messageService);
    }

    @Test
    void pinMessage_missingAccount_throwsNullPointerException() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);
        when(messageRepository.getPinnedMessagesByChatRoom("88")).thenReturn(List.of());
        when(messageRepository.findByChatRoomIdAndMessageId("88", "msg-pin")).thenReturn(Optional.of(message));
        when(messageRepository.isPinned("88", "msg-pin")).thenReturn(false);
        when(accountRepository.findByEmailAndDeletedAtIsNull("pin@example.com")).thenReturn(Optional.empty());

        assertThrows(NullPointerException.class, () -> pinnedMessageService.pinMessage(88L, "msg-pin", currentUser));
        verify(messageRepository, never()).pinMessage(any(), any(), any());
        verifyNoInteractions(messageService);
    }

    @Test
    void pinMessage_sendsPinnedEventWithPinnedMessageId() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);
        when(messageRepository.getPinnedMessagesByChatRoom("88")).thenReturn(List.of());
        when(messageRepository.findByChatRoomIdAndMessageId("88", "msg-pin")).thenReturn(Optional.of(message));
        when(messageRepository.isPinned("88", "msg-pin")).thenReturn(false);
        when(accountRepository.findByEmailAndDeletedAtIsNull("pin@example.com")).thenReturn(Optional.of(currentUser));
        when(messageRepository.pinMessage("88", "msg-pin", "Pin User")).thenReturn(pinnedMessage);

        pinnedMessageService.pinMessage(88L, "msg-pin", currentUser);

        verify(messageService).createAndSendSystemMessage(eq(88L), eq(MessageEvent.MESSAGE_PINNED), eq(currentUser), any(), eq("msg-pin"));
    }

    @Test
    void pinMessage_usesPersistedAccountLookupBeforeSaving() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);
        when(messageRepository.getPinnedMessagesByChatRoom("88")).thenReturn(List.of());
        when(messageRepository.findByChatRoomIdAndMessageId("88", "msg-pin")).thenReturn(Optional.of(message));
        when(messageRepository.isPinned("88", "msg-pin")).thenReturn(false);
        when(accountRepository.findByEmailAndDeletedAtIsNull("pin@example.com")).thenReturn(Optional.of(currentUser));
        when(messageRepository.pinMessage("88", "msg-pin", "Pin User")).thenReturn(pinnedMessage);

        pinnedMessageService.pinMessage(88L, "msg-pin", currentUser);

        verify(accountRepository).findByEmailAndDeletedAtIsNull("pin@example.com");
        verify(messageRepository).pinMessage("88", "msg-pin", "Pin User");
    }

    @Test
    void pinMessage_setsResponseFromPersistedPinnedMessage() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);
        when(messageRepository.getPinnedMessagesByChatRoom("88")).thenReturn(List.of());
        when(messageRepository.findByChatRoomIdAndMessageId("88", "msg-pin")).thenReturn(Optional.of(message));
        when(messageRepository.isPinned("88", "msg-pin")).thenReturn(false);
        when(accountRepository.findByEmailAndDeletedAtIsNull("pin@example.com")).thenReturn(Optional.of(currentUser));
        when(messageRepository.pinMessage("88", "msg-pin", "Pin User")).thenReturn(pinnedMessage);

        PinnedMessageResponse response = pinnedMessageService.pinMessage(88L, "msg-pin", currentUser);

        assertThat(response.getChatRoomId()).isEqualTo("88");
        assertThat(response.getMessageId()).isEqualTo("msg-pin");
        assertThat(response.getPinnedBy()).isEqualTo("Pin User");
    }

    @Test
    void isMessagePinned_shouldReturnRepositoryValue() {
        when(messageRepository.isPinned("88", "msg-pin")).thenReturn(true);

        assertThat(pinnedMessageService.isMessagePinned(88L, "msg-pin")).isTrue();
    }
}