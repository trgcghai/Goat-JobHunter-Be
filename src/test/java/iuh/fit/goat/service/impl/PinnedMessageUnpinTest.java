package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.MessageEvent;
import iuh.fit.goat.entity.*;
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
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PinnedMessageUnpinTest {

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
    void unpinMessage_shouldDeletePinnedMessageAndSendEvent() throws Exception {
        prepareSuccessfulUnpinWithCurrentUser();

        pinnedMessageService.unpinMessage(88L, "msg-pin", currentUser);

        verify(messageRepository).deletePinnedMessage("88", "msg-pin");
        verify(messageService).createAndSendSystemMessage(eq(88L), eq(MessageEvent.MESSAGE_UNPINNED), eq(currentUser), any(), eq("msg-pin"));
    }

    @Test
    void unpinMessage_chatRoomNotFound_throwsInvalid() {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.empty());

        assertThrows(InvalidException.class, () -> pinnedMessageService.unpinMessage(88L, "msg-pin", currentUser));
        verifyNoInteractions(messageRepository, messageService, accountRepository, chatRoomPermissionGuard);
    }

    @Test
    void unpinMessage_permissionDenied_throwsInvalid() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doThrow(new InvalidException("forbidden")).when(chatRoomPermissionGuard)
                .assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);

        assertThrows(InvalidException.class, () -> pinnedMessageService.unpinMessage(88L, "msg-pin", currentUser));
        verify(messageRepository, never()).deletePinnedMessage(any(), any());
        verifyNoInteractions(messageService);
    }

    @Test
    void unpinMessage_notPinned_throwsInvalid() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);
        when(messageRepository.getPinnedMessage("88", "msg-pin")).thenReturn(null);

        assertThrows(InvalidException.class, () -> pinnedMessageService.unpinMessage(88L, "msg-pin", currentUser));
        verify(messageRepository, never()).deletePinnedMessage(any(), any());
        verifyNoInteractions(messageService);
    }

    @Test
    void unpinMessage_messageMissingAfterDelete_throwsInvalid() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);
        when(messageRepository.getPinnedMessage("88", "msg-pin")).thenReturn(pinnedMessage);
        when(messageRepository.findByChatRoomIdAndMessageId("88", "msg-pin")).thenReturn(Optional.empty());
        when(accountRepository.findByEmailAndDeletedAtIsNull("pin@example.com")).thenReturn(Optional.of(currentUser));

        assertThrows(InvalidException.class, () -> pinnedMessageService.unpinMessage(88L, "msg-pin", currentUser));
        verify(messageRepository).deletePinnedMessage("88", "msg-pin");
        verifyNoInteractions(messageService);
    }

    @Test
    void unpinMessage_companyAccount_usesCompanyActor() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);
        when(messageRepository.getPinnedMessage("88", "msg-pin")).thenReturn(pinnedMessage);
        when(messageRepository.findByChatRoomIdAndMessageId("88", "msg-pin")).thenReturn(Optional.of(message));
        Company company = new Company();
        company.setName("GoatCo");
        when(accountRepository.findByEmailAndDeletedAtIsNull("pin@example.com")).thenReturn(Optional.of(company));

        pinnedMessageService.unpinMessage(88L, "msg-pin", currentUser);

        verify(messageService).createAndSendSystemMessage(eq(88L), eq(MessageEvent.MESSAGE_UNPINNED), eq(company), any(), eq("msg-pin"));
    }

    @Test
    void unpinMessage_missingAccount_passesNullActor() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);
        when(messageRepository.getPinnedMessage("88", "msg-pin")).thenReturn(pinnedMessage);
        when(messageRepository.findByChatRoomIdAndMessageId("88", "msg-pin")).thenReturn(Optional.of(message));
        when(accountRepository.findByEmailAndDeletedAtIsNull("pin@example.com")).thenReturn(Optional.empty());

        pinnedMessageService.unpinMessage(88L, "msg-pin", currentUser);

        verify(messageService).createAndSendSystemMessage(eq(88L), eq(MessageEvent.MESSAGE_UNPINNED), isNull(), any(), eq("msg-pin"));
    }

    @Test
    void unpinMessage_deletesBeforeSendingEvent() throws Exception {
        prepareSuccessfulUnpinWithCurrentUser();

        pinnedMessageService.unpinMessage(88L, "msg-pin", currentUser);

        InOrder inOrder = inOrder(messageRepository, messageService);
        inOrder.verify(messageRepository).deletePinnedMessage("88", "msg-pin");
        inOrder.verify(messageService).createAndSendSystemMessage(eq(88L), eq(MessageEvent.MESSAGE_UNPINNED), eq(currentUser), any(), eq("msg-pin"));
    }

    @Test
    void unpinMessage_deletesOnlyRequestedPin() throws Exception {
        prepareSuccessfulUnpinWithCurrentUser();

        pinnedMessageService.unpinMessage(88L, "msg-pin", currentUser);

        verify(messageRepository).deletePinnedMessage("88", "msg-pin");
        verify(messageRepository, never()).deletePinnedMessage("88", "other-message");
    }

    @Test
    void unpinMessage_usesPinnedMessageIdInEvent() throws Exception {
        prepareSuccessfulUnpinWithCurrentUser();

        pinnedMessageService.unpinMessage(88L, "msg-pin", currentUser);

        verify(messageService).createAndSendSystemMessage(eq(88L), eq(MessageEvent.MESSAGE_UNPINNED), eq(currentUser), any(), eq("msg-pin"));
    }

    @Test
    void unpinMessage_succeedsWithPersistedAccountLookup() throws Exception {
        prepareSuccessfulUnpinWithCurrentUser();

        pinnedMessageService.unpinMessage(88L, "msg-pin", currentUser);

        verify(accountRepository).findByEmailAndDeletedAtIsNull("pin@example.com");
        verify(messageRepository).findByChatRoomIdAndMessageId("88", "msg-pin");
    }

    private void prepareSuccessfulUnpinWithCurrentUser() throws Exception {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 700L)).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.PIN_CONTENT);
        when(messageRepository.getPinnedMessage("88", "msg-pin")).thenReturn(pinnedMessage);
        when(messageRepository.findByChatRoomIdAndMessageId("88", "msg-pin")).thenReturn(Optional.of(message));
        when(accountRepository.findByEmailAndDeletedAtIsNull("pin@example.com")).thenReturn(Optional.of(currentUser));
    }
}
