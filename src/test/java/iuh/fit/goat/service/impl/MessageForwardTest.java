package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.message.ForwardMessageRequest;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.NotFoundException;
import iuh.fit.goat.exception.PermissionException;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.service.helper.MessageHelper;
import iuh.fit.goat.service.helper.ChatRoomPermissionGuard;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageForwardTest {

    @Mock private MessageRepository messageRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private MessageHelper messageHelper;

    @InjectMocks private MessageServiceImpl messageServiceImpl;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new User();
        account.setAccountId(10L);
        account.setEmail("a@a.com");
    }

    @Test
    void forwardMessage_nullSource_throwsInvalid() {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(2L));
        assertThrows(InvalidException.class, () -> messageServiceImpl.forwardMessage(null, "m", req, account));
    }

    @Test
    void forwardMessage_blankMessageId_throwsInvalid() {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(2L));
        assertThrows(InvalidException.class, () -> messageServiceImpl.forwardMessage(1L, "", req, account));
    }

    @Test
    void forwardMessage_nullAccount_throwsInvalid() {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(2L));
        assertThrows(InvalidException.class, () -> messageServiceImpl.forwardMessage(1L, "mid", req, null));
    }

    @Test
    void forwardMessage_sourceRoomNotFound_throwsNotFound() {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(2L));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> messageServiceImpl.forwardMessage(1L, "m", req, account));
    }

    @Test
    void forwardMessage_userNotMember_throwsPermission() {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(2L));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(new ChatRoom()));
        when(messageHelper.isUserInChatRoom(1L, account.getAccountId())).thenReturn(false);

        assertThrows(PermissionException.class, () -> messageServiceImpl.forwardMessage(1L, "m", req, account));
    }

    @Test
    void forwardMessage_sourceMessageNotFound_throwsNotFound() {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(2L));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(new ChatRoom()));
        when(messageHelper.isUserInChatRoom(1L, account.getAccountId())).thenReturn(true);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> messageServiceImpl.forwardMessage(1L, "m", req, account));
    }

    @Test
    void forwardMessage_validatesForwardableSourceMessage() throws Exception {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(2L));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(new ChatRoom()));
        when(messageHelper.isUserInChatRoom(1L, account.getAccountId())).thenReturn(true);
        Message src = new Message(); src.setChatRoomId("1"); src.setMessageId("m");
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m")).thenReturn(Optional.of(src));
        lenient().doNothing().when(messageHelper).validateForwardableSourceMessage(src);
        when(messageHelper.normalizeTargetChatRoomIds(req)).thenReturn(List.of(2L));
        when(chatRoomRepository.findById(2L)).thenReturn(Optional.of(new ChatRoom()));
        when(messageHelper.isUserInChatRoom(2L, account.getAccountId())).thenReturn(true);
        Message f2 = new Message(); f2.setMessageId("f2"); f2.setChatRoomId("2");
        when(messageHelper.createForwardedMessage(src, 2L, account)).thenReturn(f2);
        when(messageHelper.saveAndDispatchMessageOptimized(2L, f2, account)).thenReturn(f2);

        var resp = messageServiceImpl.forwardMessage(1L, "m", req, account);
        assertThat(resp.getSuccessCount()).isEqualTo(1);
    }

    @Test
    void forwardMessage_targetEqualsSource_countsAsFailure() throws Exception {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(1L));
        when(messageHelper.normalizeTargetChatRoomIds(req)).thenReturn(List.of(1L));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(new ChatRoom()));
        when(messageHelper.isUserInChatRoom(1L, account.getAccountId())).thenReturn(true);
        Message src = new Message(); src.setChatRoomId("1"); src.setMessageId("m");
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m")).thenReturn(Optional.of(src));
        lenient().doNothing().when(messageHelper).validateForwardableSourceMessage(src);

        var resp = messageServiceImpl.forwardMessage(1L, "m", req, account);
        assertThat(resp.getFailedCount()).isEqualTo(1);
        assertThat(resp.getSuccessCount()).isEqualTo(0);
    }

    @Test
    void forwardMessage_targetNotFound_countsAsFailure() throws Exception {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(2L));
        when(messageHelper.normalizeTargetChatRoomIds(req)).thenReturn(List.of(2L));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(new ChatRoom()));
        when(messageHelper.isUserInChatRoom(1L, account.getAccountId())).thenReturn(true);
        Message src = new Message(); src.setChatRoomId("1"); src.setMessageId("m");
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m")).thenReturn(Optional.of(src));
        lenient().doNothing().when(messageHelper).validateForwardableSourceMessage(src);
        when(chatRoomRepository.findById(2L)).thenReturn(Optional.empty());

        var resp = messageServiceImpl.forwardMessage(1L, "m", req, account);
        assertThat(resp.getFailedCount()).isEqualTo(1);
    }

    @Test
    void forwardMessage_targetNotMember_countsAsFailure() throws Exception {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(2L));
        when(messageHelper.normalizeTargetChatRoomIds(req)).thenReturn(List.of(2L));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(new ChatRoom()));
        when(messageHelper.isUserInChatRoom(1L, account.getAccountId())).thenReturn(true);
        Message src = new Message(); src.setChatRoomId("1"); src.setMessageId("m");
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m")).thenReturn(Optional.of(src));
        lenient().doNothing().when(messageHelper).validateForwardableSourceMessage(src);
        when(chatRoomRepository.findById(2L)).thenReturn(Optional.of(new ChatRoom()));
        when(messageHelper.isUserInChatRoom(2L, account.getAccountId())).thenReturn(false);

        var resp = messageServiceImpl.forwardMessage(1L, "m", req, account);
        assertThat(resp.getFailedCount()).isEqualTo(1);
    }

    @Test
    void forwardMessage_runtimeFailureOnCreate_countsAsFailure() throws Exception {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(2L));
        when(messageHelper.normalizeTargetChatRoomIds(req)).thenReturn(List.of(2L));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(new ChatRoom()));
        when(messageHelper.isUserInChatRoom(1L, account.getAccountId())).thenReturn(true);
        Message src = new Message(); src.setChatRoomId("1"); src.setMessageId("m");
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m")).thenReturn(Optional.of(src));
        lenient().doNothing().when(messageHelper).validateForwardableSourceMessage(src);
        when(chatRoomRepository.findById(2L)).thenReturn(Optional.of(new ChatRoom()));
        when(messageHelper.isUserInChatRoom(2L, account.getAccountId())).thenReturn(true);
        when(messageHelper.createForwardedMessage(src, 2L, account)).thenThrow(new RuntimeException("boom"));

        var resp = messageServiceImpl.forwardMessage(1L, "m", req, account);
        assertThat(resp.getFailedCount()).isEqualTo(1);
    }
}
