package iuh.fit.goat.service.impl;

import iuh.fit.goat.constant.MessageConstant;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.ConflictException;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.NotFoundException;
import iuh.fit.goat.exception.PermissionException;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.service.helper.MessageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageRevokeTest {

    @Mock private MessageHelper messageHelper;
    @Mock private MessageRepository messageRepository;

    @InjectMocks private MessageServiceImpl messageService;

    private Account currentUser;
    private Message sourceMessage;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setAccountId(5L);
        currentUser.setEmail("sender@example.com");

        sourceMessage = new Message();
        sourceMessage.setMessageId("m1");
        sourceMessage.setChatRoomId("10");
        sourceMessage.setIsHidden(false);
        sourceMessage.setSenderId("5");
    }

    @Test
    void revokeMessage_nullChatRoomId_throwsInvalid() {
        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageService.revokeMessage(null, "m1", currentUser)
        );
    }

    @Test
    void revokeMessage_blankMessageId_throwsInvalid() {
        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageService.revokeMessage(10L, "  ", currentUser)
        );
    }

    @Test
    void revokeMessage_nullAccount_throwsInvalid() {
        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageService.revokeMessage(10L, "m1", null)
        );
    }

    @Test
    void revokeMessage_messageNotFound_throwsNotFound() {
        when(messageRepository.findByChatRoomIdAndMessageId("10", "m1")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(NotFoundException.class, () ->
                messageService.revokeMessage(10L, "m1", currentUser)
        );
    }

    @Test
    void revokeMessage_alreadyHidden_throwsConflict() {
        sourceMessage.setIsHidden(true);
        when(messageRepository.findByChatRoomIdAndMessageId("10", "m1")).thenReturn(Optional.of(sourceMessage));

        org.junit.jupiter.api.Assertions.assertThrows(ConflictException.class, () ->
                messageService.revokeMessage(10L, "m1", currentUser)
        );
    }

    @Test
    void revokeMessage_missingSenderInformation_throwsInvalid() {
        sourceMessage.setSenderId(null);
        sourceMessage.setSender(null);
        when(messageRepository.findByChatRoomIdAndMessageId("10", "m1")).thenReturn(Optional.of(sourceMessage));
        when(messageHelper.extractSenderAccountId(sourceMessage)).thenReturn(null);

        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageService.revokeMessage(10L, "m1", currentUser)
        );
    }

    @Test
    void revokeMessage_notSender_throwsPermission() {
        when(messageRepository.findByChatRoomIdAndMessageId("10", "m1")).thenReturn(Optional.of(sourceMessage));
        when(messageHelper.extractSenderAccountId(sourceMessage)).thenReturn(99L);

        org.junit.jupiter.api.Assertions.assertThrows(PermissionException.class, () ->
                messageService.revokeMessage(10L, "m1", currentUser)
        );
    }

    @Test
    void revokeMessage_applyRecallFails_throwsInvalid() {
        when(messageRepository.findByChatRoomIdAndMessageId("10", "m1")).thenReturn(Optional.of(sourceMessage));
        when(messageHelper.extractSenderAccountId(sourceMessage)).thenReturn(5L);
        when(messageHelper.collectCascadeRecallMessages(sourceMessage)).thenReturn(List.of(sourceMessage));
        doThrow(new RuntimeException("boom")).when(messageHelper).applyRecallState(List.of(sourceMessage));

        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageService.revokeMessage(10L, "m1", currentUser)
        );

        verify(messageHelper, never()).sendMessageToUsers(anyString(), any());
    }

    @Test
    void revokeMessage_success_singleMessage_sendsEventAndRefreshesSummary() throws Exception {
        when(messageRepository.findByChatRoomIdAndMessageId("10", "m1")).thenReturn(Optional.of(sourceMessage));
        when(messageHelper.extractSenderAccountId(sourceMessage)).thenReturn(5L);
        when(messageHelper.collectCascadeRecallMessages(sourceMessage)).thenReturn(List.of(sourceMessage));
        when(messageHelper.applyRecallState(List.of(sourceMessage))).thenReturn(List.of(sourceMessage));

        Message revoked = messageService.revokeMessage(10L, "m1", currentUser);

        assertThat(revoked).isEqualTo(sourceMessage);
        verify(messageHelper).sendMessageToUsers(eq("10"), eq(sourceMessage));
        verify(messageHelper).refreshChatRoomSummaries(eq(java.util.Set.of("10")));
    }

    @Test
    void revokeMessage_success_multipleMessages_sendsEachEvent() throws Exception {
        Message child = new Message();
        child.setMessageId("m1-forward");
        child.setChatRoomId("11");
        child.setIsHidden(false);
        child.setSenderId("5");

        when(messageRepository.findByChatRoomIdAndMessageId("10", "m1")).thenReturn(Optional.of(sourceMessage));
        when(messageHelper.extractSenderAccountId(sourceMessage)).thenReturn(5L);
        when(messageHelper.collectCascadeRecallMessages(sourceMessage)).thenReturn(List.of(sourceMessage, child));
        when(messageHelper.applyRecallState(List.of(sourceMessage, child))).thenReturn(List.of(sourceMessage, child));

        Message revoked = messageService.revokeMessage(10L, "m1", currentUser);

        assertThat(revoked).isEqualTo(sourceMessage);
        verify(messageHelper).sendMessageToUsers("10", sourceMessage);
        verify(messageHelper).sendMessageToUsers("11", child);
        verify(messageHelper).refreshChatRoomSummaries(eq(java.util.Set.of("10", "11")));
    }

    @Test
    void revokeMessage_rootMissingFromRecalledList_returnsOriginalMessage() throws Exception {
        Message child = new Message();
        child.setMessageId("m1-child");
        child.setChatRoomId("10");
        child.setIsHidden(false);

        when(messageRepository.findByChatRoomIdAndMessageId("10", "m1")).thenReturn(Optional.of(sourceMessage));
        when(messageHelper.extractSenderAccountId(sourceMessage)).thenReturn(5L);
        when(messageHelper.collectCascadeRecallMessages(sourceMessage)).thenReturn(List.of(child));
        when(messageHelper.applyRecallState(List.of(child))).thenReturn(List.of(child));

        Message revoked = messageService.revokeMessage(10L, "m1", currentUser);

        assertThat(revoked).isEqualTo(sourceMessage);
        verify(messageHelper).sendMessageToUsers("10", child);
        verify(messageHelper).refreshChatRoomSummaries(eq(java.util.Set.of("10")));
    }

    @Test
    void revokeMessage_refreshesUniqueChatRoomIds_onlyOncePerRoom() throws Exception {
        Message sameRoomChild = new Message();
        sameRoomChild.setMessageId("m1-child");
        sameRoomChild.setChatRoomId("10");
        sameRoomChild.setIsHidden(false);

        when(messageRepository.findByChatRoomIdAndMessageId("10", "m1")).thenReturn(Optional.of(sourceMessage));
        when(messageHelper.extractSenderAccountId(sourceMessage)).thenReturn(5L);
        when(messageHelper.collectCascadeRecallMessages(sourceMessage)).thenReturn(List.of(sourceMessage, sameRoomChild));
        when(messageHelper.applyRecallState(List.of(sourceMessage, sameRoomChild))).thenReturn(List.of(sourceMessage, sameRoomChild));

        messageService.revokeMessage(10L, "m1", currentUser);

        ArgumentCaptor<java.util.Collection<String>> captor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(messageHelper).refreshChatRoomSummaries(captor.capture());
        assertThat(new java.util.LinkedHashSet<>(captor.getValue())).containsExactly("10");
    }

    @Test
    void revokeMessage_validateChatRoomIsCalledBeforeLookup() throws Exception {
        when(messageRepository.findByChatRoomIdAndMessageId("10", "m1")).thenReturn(Optional.of(sourceMessage));
        when(messageHelper.extractSenderAccountId(sourceMessage)).thenReturn(5L);
        when(messageHelper.collectCascadeRecallMessages(sourceMessage)).thenReturn(List.of(sourceMessage));
        when(messageHelper.applyRecallState(List.of(sourceMessage))).thenReturn(List.of(sourceMessage));

        messageService.revokeMessage(10L, "m1", currentUser);

        verify(messageHelper).validateChatRoom(10L);
        verify(messageRepository).findByChatRoomIdAndMessageId("10", "m1");
    }
}