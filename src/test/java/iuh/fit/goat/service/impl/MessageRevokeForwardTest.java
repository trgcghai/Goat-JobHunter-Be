package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.message.ForwardMessageRequest;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.service.helper.MessageHelper;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.dto.response.message.ForwardMessageResponse;
import iuh.fit.goat.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import iuh.fit.goat.exception.ConflictException;
import iuh.fit.goat.exception.PermissionException;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageRevokeForwardTest {

    @Mock private MessageHelper messageHelper;
    @Mock private MessageRepository messageRepository;
    @Mock private ChatRoomRepository chatRoomRepository;

    @InjectMocks private MessageServiceImpl messageService;

    private Account sender;
    private Message source;

    @BeforeEach
    void setUp() {
        sender = new User(); sender.setAccountId(11L);
        source = new Message(); source.setMessageId("m1"); source.setChatRoomId("10"); source.setIsHidden(false);
    }

    @Test
    void revokeMessage_shouldReturnRevokedMessage_whenSenderMatches() throws Exception {
        when(messageRepository.findByChatRoomIdAndMessageId("10", "m1")).thenReturn(Optional.of(source));
        when(messageHelper.extractSenderAccountId(source)).thenReturn(11L);
        when(messageHelper.collectCascadeRecallMessages(source)).thenReturn(List.of(source));
        when(messageHelper.applyRecallState(any())).thenReturn(List.of(source));
        // no need to stub toMessageResponse here

        var revoked = messageService.revokeMessage(10L, "m1", sender);
        assertThat(revoked).isNotNull();
        verify(messageHelper).sendMessageToUsers(anyString(), any(Message.class));
    }

    @Test
    void revokeMessage_shouldThrowPermissionWhenSenderDiffers() {
        when(messageRepository.findByChatRoomIdAndMessageId("10", "m1")).thenReturn(Optional.of(source));
        when(messageHelper.extractSenderAccountId(source)).thenReturn(999L);

        org.junit.jupiter.api.Assertions.assertThrows(PermissionException.class, () ->
                messageService.revokeMessage(10L, "m1", sender)
        );
    }

    @Test
    void revokeMessage_shouldThrowConflictWhenAlreadyHidden() {
        source.setIsHidden(true);
        when(messageRepository.findByChatRoomIdAndMessageId("10", "m1")).thenReturn(Optional.of(source));

        org.junit.jupiter.api.Assertions.assertThrows(ConflictException.class, () ->
                messageService.revokeMessage(10L, "m1", sender)
        );
    }

    @Test
    void forwardMessage_shouldBuildFailuresForMissingTarget() throws Exception {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(99L));
        lenient().when(messageHelper.normalizeTargetChatRoomIds(req)).thenReturn(List.of(99L));
        lenient().when(chatRoomRepository.findById(99L)).thenReturn(Optional.empty());
        lenient().when(messageRepository.findByChatRoomIdAndMessageId("10","m1")).thenReturn(Optional.of(source));
        lenient().when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(new iuh.fit.goat.entity.ChatRoom()));
        lenient().when(messageHelper.isUserInChatRoom(10L, 11L)).thenReturn(true);
        lenient().doNothing().when(messageHelper).validateForwardableSourceMessage(source);

        var resp = messageService.forwardMessage(10L, "m1", req, sender);
        assertThat(resp.getFailedCount()).isGreaterThan(0);
    }

    @Test
    void forwardMessage_sourceNotFound_throwsNotFound() {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(2L));
        when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(new iuh.fit.goat.entity.ChatRoom()));
        when(messageHelper.isUserInChatRoom(10L, 11L)).thenReturn(true);
        when(messageRepository.findByChatRoomIdAndMessageId("10","m1")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(NotFoundException.class, () ->
                messageService.forwardMessage(10L, "m1", req, sender)
        );
    }

    @Test
    void forwardMessage_userNotInSource_throwsPermission() {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(2L));
        when(messageRepository.findByChatRoomIdAndMessageId("10","m1")).thenReturn(Optional.of(source));
        when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(new iuh.fit.goat.entity.ChatRoom()));
        when(messageHelper.isUserInChatRoom(10L, 11L)).thenReturn(false);

        org.junit.jupiter.api.Assertions.assertThrows(PermissionException.class, () ->
                messageService.forwardMessage(10L, "m1", req, sender)
        );
    }

    @Test
    void forwardMessage_validateForwardableThrows_invalid() throws Exception {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(2L));
        when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(new iuh.fit.goat.entity.ChatRoom()));
        when(messageHelper.isUserInChatRoom(10L, 11L)).thenReturn(true);
        when(messageRepository.findByChatRoomIdAndMessageId("10","m1")).thenReturn(Optional.of(source));
        doThrow(new InvalidException("not forwardable")).when(messageHelper).validateForwardableSourceMessage(source);

        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
            messageService.forwardMessage(10L, "m1", req, sender)
        );
    }

    @Test
    void revokeMessage_nullInputs_throwInvalid() {
        org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                messageService.revokeMessage(null, "m", sender)
        );
    }
}
