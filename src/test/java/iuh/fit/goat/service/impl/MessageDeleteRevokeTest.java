package iuh.fit.goat.service.impl;

import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.NotFoundException;
import iuh.fit.goat.service.helper.MessageHelper;
import iuh.fit.goat.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class MessageDeleteRevokeTest {

    @Mock private MessageHelper messageHelper;
    @Mock private MessageRepository messageRepository;

    @InjectMocks private MessageServiceImpl messageServiceImpl;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new User(); account.setAccountId(5L);
    }

    @Test
    void deleteMessagePermanently_shouldThrowWhenNotFound() {
        when(messageRepository.findByChatRoomIdAndMessageId(anyString(), anyString())).thenReturn(Optional.empty());
        org.junit.jupiter.api.Assertions.assertThrows(NotFoundException.class, () ->
            messageServiceImpl.deleteMessagePermanently(1L, "no", account)
        );
    }

    @Test
    void deleteMessagePermanently_shouldDeleteAndEmitEvents() throws Exception {
        Message root = new Message(); root.setMessageId("r1"); root.setChatRoomId("1");

        MessageHelper.CascadeDeleteTarget target = new MessageHelper.CascadeDeleteTarget(root, iuh.fit.goat.constant.MessageConstant.DELETE_TYPE_FORWARDED);
        when(messageRepository.findByChatRoomIdAndMessageId("1","r1")).thenReturn(Optional.of(root));
        when(messageHelper.extractSenderAccountId(root)).thenReturn(5L);
        when(messageHelper.isSuperAdmin(account)).thenReturn(false);
        when(messageHelper.collectCascadeDeleteTargets(root)).thenReturn(List.of(target));
        // delete succeeds
        doNothing().when(messageHelper).deleteSingleMessagePermanently(root);
        iuh.fit.goat.dto.response.message.MessageDeletedEventResponse resp = new iuh.fit.goat.dto.response.message.MessageDeletedEventResponse();
        resp.setMessageId(root.getMessageId());
        resp.setChatRoomId(root.getChatRoomId());
        when(messageHelper.buildMessageDeletedEvent(eq(root), any(), eq(5L), any())).thenReturn(resp);

        var ev = messageServiceImpl.deleteMessagePermanently(1L, "r1", account);
        // root event returned
        assertThat(ev).isNotNull();
        verify(messageHelper).sendMessageDeletedEvent(eq(root.getChatRoomId()), any());
    }

    @Test
    void deleteMessagePermanently_shouldThrowPermissionWhenNotSenderOrAdmin() {
        Message root = new Message(); root.setMessageId("r2"); root.setChatRoomId("1");
        when(messageRepository.findByChatRoomIdAndMessageId("1","r2")).thenReturn(Optional.of(root));
        when(messageHelper.extractSenderAccountId(root)).thenReturn(999L);
        when(messageHelper.isSuperAdmin(account)).thenReturn(false);

        org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.PermissionException.class, () ->
                messageServiceImpl.deleteMessagePermanently(1L, "r2", account)
        );
    }

    @Test
    void deleteMessagePermanently_shouldLogAndThrowWhenCascadeDeleteFails() throws Exception {
        Message root = new Message(); root.setMessageId("r3"); root.setChatRoomId("1");
        MessageHelper.CascadeDeleteTarget target = new MessageHelper.CascadeDeleteTarget(root, iuh.fit.goat.constant.MessageConstant.DELETE_TYPE_FORWARDED);

        when(messageRepository.findByChatRoomIdAndMessageId("1","r3")).thenReturn(Optional.of(root));
        when(messageHelper.extractSenderAccountId(root)).thenReturn(5L);
        when(messageHelper.isSuperAdmin(account)).thenReturn(false);
        when(messageHelper.collectCascadeDeleteTargets(root)).thenReturn(List.of(target));
        doThrow(new iuh.fit.goat.exception.InvalidException("bad")).when(messageHelper).deleteSingleMessagePermanently(root);

        org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                messageServiceImpl.deleteMessagePermanently(1L, "r3", account)
        );

        verify(messageHelper, never()).sendMessageDeletedEvent(anyString(), any());
    }

    @Test
    void deleteMessagePermanently_shouldAllowAdminToDelete() throws Exception {
        Message root = new Message(); root.setMessageId("admin1"); root.setChatRoomId("2");
        MessageHelper.CascadeDeleteTarget target = new MessageHelper.CascadeDeleteTarget(root, iuh.fit.goat.constant.MessageConstant.DELETE_TYPE_FORWARDED);

        when(messageRepository.findByChatRoomIdAndMessageId("2","admin1")).thenReturn(Optional.of(root));
        when(messageHelper.extractSenderAccountId(root)).thenReturn(999L);
        when(messageHelper.isSuperAdmin(account)).thenReturn(true);
        when(messageHelper.collectCascadeDeleteTargets(root)).thenReturn(List.of(target));
        doNothing().when(messageHelper).deleteSingleMessagePermanently(root);
        iuh.fit.goat.dto.response.message.MessageDeletedEventResponse respAdmin = new iuh.fit.goat.dto.response.message.MessageDeletedEventResponse();
        respAdmin.setMessageId(root.getMessageId());
        respAdmin.setChatRoomId(root.getChatRoomId());
        when(messageHelper.buildMessageDeletedEvent(eq(root), any(), anyLong(), any())).thenReturn(respAdmin);

        var ev = messageServiceImpl.deleteMessagePermanently(2L, "admin1", account);
        assertThat(ev).isNotNull();
        verify(messageHelper).sendMessageDeletedEvent(eq(root.getChatRoomId()), any());
    }

    @Test
    void deleteMessagePermanently_shouldRejectNullChatRoomId() {
        org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                messageServiceImpl.deleteMessagePermanently(null, "m", account)
        );
    }

    @Test
    void deleteMessagePermanently_shouldRejectBlankMessageId() {
        org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                messageServiceImpl.deleteMessagePermanently(1L, "", account)
        );
    }

    @Test
    void deleteMessagePermanently_shouldRejectNullAccount() {
        org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                messageServiceImpl.deleteMessagePermanently(1L, "m", null)
        );
    }

    @Test
    void deleteMessagePermanently_multipleTargets_emitsMultipleEvents() throws Exception {
        Message root = new Message(); root.setMessageId("multi1"); root.setChatRoomId("3");
        Message child = new Message(); child.setMessageId("c1"); child.setChatRoomId("3");

        MessageHelper.CascadeDeleteTarget tRoot = new MessageHelper.CascadeDeleteTarget(root, iuh.fit.goat.constant.MessageConstant.DELETE_TYPE_ORIGINAL);
        MessageHelper.CascadeDeleteTarget tChild = new MessageHelper.CascadeDeleteTarget(child, iuh.fit.goat.constant.MessageConstant.DELETE_TYPE_FORWARDED);

        when(messageRepository.findByChatRoomIdAndMessageId("3","multi1")).thenReturn(Optional.of(root));
        when(messageHelper.extractSenderAccountId(root)).thenReturn(5L);
        when(messageHelper.isSuperAdmin(account)).thenReturn(false);
        when(messageHelper.collectCascadeDeleteTargets(root)).thenReturn(List.of(tRoot, tChild));
        doNothing().when(messageHelper).deleteSingleMessagePermanently(any());

        iuh.fit.goat.dto.response.message.MessageDeletedEventResponse respRoot = new iuh.fit.goat.dto.response.message.MessageDeletedEventResponse();
        respRoot.setMessageId(root.getMessageId()); respRoot.setChatRoomId(root.getChatRoomId());
        iuh.fit.goat.dto.response.message.MessageDeletedEventResponse respChild = new iuh.fit.goat.dto.response.message.MessageDeletedEventResponse();
        respChild.setMessageId(child.getMessageId()); respChild.setChatRoomId(child.getChatRoomId());
        when(messageHelper.buildMessageDeletedEvent(eq(root), any(), eq(5L), any())).thenReturn(respRoot);
        when(messageHelper.buildMessageDeletedEvent(eq(child), any(), eq(5L), any())).thenReturn(respChild);

        var ev = messageServiceImpl.deleteMessagePermanently(3L, "multi1", account);
        assertThat(ev).isNotNull();
        verify(messageHelper, atLeast(2)).sendMessageDeletedEvent(anyString(), any());
    }

    @Test
    void deleteMessagePermanently_noTargets_throwsInvalid() throws Exception {
        Message root = new Message(); root.setMessageId("none"); root.setChatRoomId("4");
        when(messageRepository.findByChatRoomIdAndMessageId("4","none")).thenReturn(Optional.of(root));
        when(messageHelper.extractSenderAccountId(root)).thenReturn(5L);
        when(messageHelper.isSuperAdmin(account)).thenReturn(false);
        when(messageHelper.collectCascadeDeleteTargets(root)).thenReturn(List.of());

        org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                messageServiceImpl.deleteMessagePermanently(4L, "none", account)
        );
    }

}
