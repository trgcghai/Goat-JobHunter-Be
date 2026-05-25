package iuh.fit.goat.service.impl;

import iuh.fit.goat.constant.MessageConstant;
import iuh.fit.goat.dto.response.message.MessageDeletedEventResponse;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageDeleteTest {

    @Mock private MessageHelper messageHelper;
    @Mock private MessageRepository messageRepository;

    @InjectMocks
    private MessageServiceImpl messageService;

    private User currentUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setAccountId(5L);
        currentUser.setEmail("sender@example.com");

        adminUser = new User();
        adminUser.setAccountId(1L);
        adminUser.setEmail("admin@example.com");
    }

    @Test
    void deleteMessagePermanently_nullChatRoomId_throwsInvalid() {
        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageService.deleteMessagePermanently(null, "m1", currentUser)
        );
    }

    @Test
    void deleteMessagePermanently_blankMessageId_throwsInvalid() {
        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageService.deleteMessagePermanently(1L, "  ", currentUser)
        );
    }

    @Test
    void deleteMessagePermanently_nullAccount_throwsInvalid() {
        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageService.deleteMessagePermanently(1L, "m1", null)
        );
    }

    @Test
        void deleteMessagePermanently_messageNotFound_throwsNotFound() throws Exception {
        doNothing().when(messageHelper).validateChatRoom(1L);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m1")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(NotFoundException.class, () ->
                messageService.deleteMessagePermanently(1L, "m1", currentUser)
        );
    }

    @Test
        void deleteMessagePermanently_notSenderOrAdmin_throwsPermission() throws Exception {
        Message root = new Message();
        root.setMessageId("m2");
        root.setChatRoomId("1");
        doNothing().when(messageHelper).validateChatRoom(1L);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m2")).thenReturn(Optional.of(root));
        when(messageHelper.extractSenderAccountId(root)).thenReturn(99L);
        when(messageHelper.isSuperAdmin(currentUser)).thenReturn(false);

        org.junit.jupiter.api.Assertions.assertThrows(PermissionException.class, () ->
                messageService.deleteMessagePermanently(1L, "m2", currentUser)
        );
    }

    @Test
    void deleteMessagePermanently_successAsSender_deletesRootAndEmitsEvent() throws Exception {
        Message root = new Message();
        root.setMessageId("m3");
        root.setChatRoomId("1");

        MessageHelper.CascadeDeleteTarget target = new MessageHelper.CascadeDeleteTarget(root, MessageConstant.DELETE_TYPE_ORIGINAL);
        MessageDeletedEventResponse event = MessageDeletedEventResponse.builder()
                .eventType(MessageConstant.MESSAGE_DELETED_EVENT)
                .chatRoomId("1")
                .messageId("m3")
                .deleteType(MessageConstant.DELETE_TYPE_ORIGINAL)
                .deletedByAccountId(5L)
                .build();

        doNothing().when(messageHelper).validateChatRoom(1L);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m3")).thenReturn(Optional.of(root));
        when(messageHelper.extractSenderAccountId(root)).thenReturn(5L);
        when(messageHelper.isSuperAdmin(currentUser)).thenReturn(false);
        when(messageHelper.collectCascadeDeleteTargets(root)).thenReturn(List.of(target));
        doNothing().when(messageHelper).deleteSingleMessagePermanently(root);
        when(messageHelper.buildMessageDeletedEvent(eq(root), eq(MessageConstant.DELETE_TYPE_ORIGINAL), eq(5L), any()))
                .thenReturn(event);

        MessageDeletedEventResponse deleted = messageService.deleteMessagePermanently(1L, "m3", currentUser);

        assertThat(deleted).isEqualTo(event);
        verify(messageHelper).validateChatRoom(1L);
        verify(messageHelper).deleteSingleMessagePermanently(root);
        verify(messageHelper).sendMessageDeletedEvent("1", event);
    }

    @Test
    void deleteMessagePermanently_successAsAdmin_deletesEvenWhenSenderDiffers() throws Exception {
        Message root = new Message();
        root.setMessageId("admin1");
        root.setChatRoomId("2");

        MessageHelper.CascadeDeleteTarget target = new MessageHelper.CascadeDeleteTarget(root, MessageConstant.DELETE_TYPE_ORIGINAL);
        MessageDeletedEventResponse event = MessageDeletedEventResponse.builder()
                .eventType(MessageConstant.MESSAGE_DELETED_EVENT)
                .chatRoomId("2")
                .messageId("admin1")
                .deleteType(MessageConstant.DELETE_TYPE_ORIGINAL)
                .deletedByAccountId(1L)
                .build();

        doNothing().when(messageHelper).validateChatRoom(2L);
        when(messageRepository.findByChatRoomIdAndMessageId("2", "admin1")).thenReturn(Optional.of(root));
        when(messageHelper.extractSenderAccountId(root)).thenReturn(99L);
        when(messageHelper.isSuperAdmin(adminUser)).thenReturn(true);
        when(messageHelper.collectCascadeDeleteTargets(root)).thenReturn(List.of(target));
        doNothing().when(messageHelper).deleteSingleMessagePermanently(root);
        when(messageHelper.buildMessageDeletedEvent(eq(root), eq(MessageConstant.DELETE_TYPE_ORIGINAL), eq(1L), any()))
                .thenReturn(event);

        MessageDeletedEventResponse deleted = messageService.deleteMessagePermanently(2L, "admin1", adminUser);

        assertThat(deleted.getMessageId()).isEqualTo("admin1");
        assertThat(deleted.getDeletedByAccountId()).isEqualTo(1L);
        verify(messageHelper).sendMessageDeletedEvent("2", event);
    }

    @Test
    void deleteMessagePermanently_cascadeDeleteFails_throwsInvalid() throws Exception {
        Message root = new Message();
        root.setMessageId("m4");
        root.setChatRoomId("1");

        MessageHelper.CascadeDeleteTarget target = new MessageHelper.CascadeDeleteTarget(root, MessageConstant.DELETE_TYPE_ORIGINAL);

        doNothing().when(messageHelper).validateChatRoom(1L);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m4")).thenReturn(Optional.of(root));
        when(messageHelper.extractSenderAccountId(root)).thenReturn(5L);
        when(messageHelper.isSuperAdmin(currentUser)).thenReturn(false);
        when(messageHelper.collectCascadeDeleteTargets(root)).thenReturn(List.of(target));
        doThrow(new InvalidException("bad delete")).when(messageHelper).deleteSingleMessagePermanently(root);

        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageService.deleteMessagePermanently(1L, "m4", currentUser)
        );

        verify(messageHelper, never()).sendMessageDeletedEvent(anyString(), any());
    }

    @Test
    void deleteMessagePermanently_rootEventMissing_throwsInvalid() throws Exception {
        Message root = new Message();
        root.setMessageId("m5");
        root.setChatRoomId("1");

        Message child = new Message();
        child.setMessageId("c5");
        child.setChatRoomId("1");

        MessageHelper.CascadeDeleteTarget childTarget = new MessageHelper.CascadeDeleteTarget(child, MessageConstant.DELETE_TYPE_FORWARDED);

        doNothing().when(messageHelper).validateChatRoom(1L);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m5")).thenReturn(Optional.of(root));
        when(messageHelper.extractSenderAccountId(root)).thenReturn(5L);
        when(messageHelper.isSuperAdmin(currentUser)).thenReturn(false);
        when(messageHelper.collectCascadeDeleteTargets(root)).thenReturn(List.of(childTarget));
        doNothing().when(messageHelper).deleteSingleMessagePermanently(child);
        when(messageHelper.buildMessageDeletedEvent(eq(child), eq(MessageConstant.DELETE_TYPE_FORWARDED), eq(5L), any()))
                .thenReturn(MessageDeletedEventResponse.builder().chatRoomId("1").messageId("c5").build());

        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageService.deleteMessagePermanently(1L, "m5", currentUser)
        );

        verify(messageHelper).sendMessageDeletedEvent(eq("1"), any());
    }

    @Test
    void deleteMessagePermanently_multipleTargets_emitsAllEventsAndRefreshesSummaries() throws Exception {
        Message root = new Message();
        root.setMessageId("m6");
        root.setChatRoomId("1");

        Message child1 = new Message();
        child1.setMessageId("c6a");
        child1.setChatRoomId("1");

        Message child2 = new Message();
        child2.setMessageId("c6b");
        child2.setChatRoomId("2");

        MessageHelper.CascadeDeleteTarget target1 = new MessageHelper.CascadeDeleteTarget(child1, MessageConstant.DELETE_TYPE_FORWARDED);
        MessageHelper.CascadeDeleteTarget target2 = new MessageHelper.CascadeDeleteTarget(child2, MessageConstant.DELETE_TYPE_FORWARDED);
        MessageHelper.CascadeDeleteTarget target3 = new MessageHelper.CascadeDeleteTarget(root, MessageConstant.DELETE_TYPE_ORIGINAL);

        doNothing().when(messageHelper).validateChatRoom(1L);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m6")).thenReturn(Optional.of(root));
        when(messageHelper.extractSenderAccountId(root)).thenReturn(5L);
        when(messageHelper.isSuperAdmin(currentUser)).thenReturn(false);
        when(messageHelper.collectCascadeDeleteTargets(root)).thenReturn(List.of(target1, target2, target3));
        doNothing().when(messageHelper).deleteSingleMessagePermanently(any(Message.class));
        when(messageHelper.buildMessageDeletedEvent(eq(child1), eq(MessageConstant.DELETE_TYPE_FORWARDED), eq(5L), any()))
                .thenReturn(MessageDeletedEventResponse.builder().chatRoomId("1").messageId("c6a").build());
        when(messageHelper.buildMessageDeletedEvent(eq(child2), eq(MessageConstant.DELETE_TYPE_FORWARDED), eq(5L), any()))
                .thenReturn(MessageDeletedEventResponse.builder().chatRoomId("2").messageId("c6b").build());
        when(messageHelper.buildMessageDeletedEvent(eq(root), eq(MessageConstant.DELETE_TYPE_ORIGINAL), eq(5L), any()))
                .thenReturn(MessageDeletedEventResponse.builder().chatRoomId("1").messageId("m6").build());

        MessageDeletedEventResponse deleted = messageService.deleteMessagePermanently(1L, "m6", currentUser);

        assertThat(deleted.getMessageId()).isEqualTo("m6");
        verify(messageHelper, org.mockito.Mockito.times(3)).sendMessageDeletedEvent(anyString(), any());
    }

    @Test
    void deleteMessagePermanently_refreshesUniqueChatRoomIds() throws Exception {
        Message root = new Message();
        root.setMessageId("m7");
        root.setChatRoomId("1");

        Message child = new Message();
        child.setMessageId("c7");
        child.setChatRoomId("1");

        MessageHelper.CascadeDeleteTarget childTarget = new MessageHelper.CascadeDeleteTarget(child, MessageConstant.DELETE_TYPE_FORWARDED);
        MessageHelper.CascadeDeleteTarget rootTarget = new MessageHelper.CascadeDeleteTarget(root, MessageConstant.DELETE_TYPE_ORIGINAL);

        doNothing().when(messageHelper).validateChatRoom(1L);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m7")).thenReturn(Optional.of(root));
        when(messageHelper.extractSenderAccountId(root)).thenReturn(5L);
        when(messageHelper.isSuperAdmin(currentUser)).thenReturn(false);
        when(messageHelper.collectCascadeDeleteTargets(root)).thenReturn(List.of(childTarget, rootTarget));
        doNothing().when(messageHelper).deleteSingleMessagePermanently(any(Message.class));
        when(messageHelper.buildMessageDeletedEvent(any(Message.class), anyString(), eq(5L), any()))
                .thenAnswer(invocation -> {
                    Message m = invocation.getArgument(0);
                    return MessageDeletedEventResponse.builder().chatRoomId(m.getChatRoomId()).messageId(m.getMessageId()).build();
                });

        messageService.deleteMessagePermanently(1L, "m7", currentUser);

        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(messageHelper).refreshChatRoomSummaries(captor.capture());
        assertThat(new LinkedHashSet<>(captor.getValue())).containsExactly("1");
    }

    @Test
    void deleteMessagePermanently_deleteTypeIsRootOriginal() throws Exception {
        Message root = new Message();
        root.setMessageId("m8");
        root.setChatRoomId("4");

        MessageHelper.CascadeDeleteTarget target = new MessageHelper.CascadeDeleteTarget(root, MessageConstant.DELETE_TYPE_ORIGINAL);
        MessageDeletedEventResponse event = MessageDeletedEventResponse.builder()
                .chatRoomId("4")
                .messageId("m8")
                .deleteType(MessageConstant.DELETE_TYPE_ORIGINAL)
                .build();

        doNothing().when(messageHelper).validateChatRoom(4L);
        when(messageRepository.findByChatRoomIdAndMessageId("4", "m8")).thenReturn(Optional.of(root));
        when(messageHelper.extractSenderAccountId(root)).thenReturn(5L);
        when(messageHelper.isSuperAdmin(currentUser)).thenReturn(false);
        when(messageHelper.collectCascadeDeleteTargets(root)).thenReturn(List.of(target));
        doNothing().when(messageHelper).deleteSingleMessagePermanently(root);
        when(messageHelper.buildMessageDeletedEvent(eq(root), eq(MessageConstant.DELETE_TYPE_ORIGINAL), eq(5L), any()))
                .thenReturn(event);

        MessageDeletedEventResponse deleted = messageService.deleteMessagePermanently(4L, "m8", currentUser);

        assertThat(deleted.getDeleteType()).isEqualTo(MessageConstant.DELETE_TYPE_ORIGINAL);
    }

    @Test
    void deleteMessagePermanently_validateChatRoomIsInvokedBeforeLookup() throws Exception {
        Message root = new Message();
        root.setMessageId("m9");
        root.setChatRoomId("9");

        doNothing().when(messageHelper).validateChatRoom(9L);
        when(messageRepository.findByChatRoomIdAndMessageId("9", "m9")).thenReturn(Optional.of(root));
        when(messageHelper.extractSenderAccountId(root)).thenReturn(5L);
        when(messageHelper.isSuperAdmin(currentUser)).thenReturn(false);
        when(messageHelper.collectCascadeDeleteTargets(root)).thenReturn(List.of(new MessageHelper.CascadeDeleteTarget(root, MessageConstant.DELETE_TYPE_ORIGINAL)));
        doNothing().when(messageHelper).deleteSingleMessagePermanently(root);
        when(messageHelper.buildMessageDeletedEvent(any(Message.class), anyString(), eq(5L), any()))
                .thenReturn(MessageDeletedEventResponse.builder().chatRoomId("9").messageId("m9").build());

        messageService.deleteMessagePermanently(9L, "m9", currentUser);

        verify(messageHelper).validateChatRoom(9L);
        verify(messageRepository).findByChatRoomIdAndMessageId("9", "m9");
    }
}