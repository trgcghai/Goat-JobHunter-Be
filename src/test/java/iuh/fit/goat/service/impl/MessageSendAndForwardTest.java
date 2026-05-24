package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.message.ForwardMessageRequest;
import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.ChatRole;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.repository.MessageHiddenRepository;
import iuh.fit.goat.service.StorageService;
import iuh.fit.goat.service.AiService;
import iuh.fit.goat.service.cache.ChatRoomCacheService;
import iuh.fit.goat.service.helper.ChatRoomPermissionGuard;
import iuh.fit.goat.service.helper.MessageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.PermissionException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageSendAndForwardTest {

    @Mock private StorageService storageService;
    @Mock private AiService aiService;
    @Mock private MessageHiddenRepository messageHiddenRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChatRoomCacheService chatRoomCache;
    @Mock private MessageHelper messageHelper;
    @Mock private ChatRoomPermissionGuard chatRoomPermissionGuard;

    @InjectMocks private MessageServiceImpl messageServiceImpl;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new User();
        account.setAccountId(10L);
        account.setEmail("a@a.com");
    }

    @Test
    void sendMessage_shouldSaveAndDispatch() throws Exception {
        MessageCreateRequest req = new MessageCreateRequest("hello", null);
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setRoomId(1L);
        ChatMember m = new ChatMember(); m.setAccount(account); m.setRole(ChatRole.MEMBER);

        when(messageHelper.getChatRoom(1L)).thenReturn(chatRoom);
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, account.getAccountId())).thenReturn(m);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(any(), any(), any());
        when(messageHelper.buildSenderInfo(account)).thenReturn(null);
        when(messageHelper.saveAndDispatchMessageOptimized(eq(1L), any(Message.class), eq(account))).thenAnswer(i -> i.getArgument(1));

        Message created = messageServiceImpl.sendMessage(1L, req, account);
        assertThat(created).isNotNull();
        verify(messageHelper).saveAndDispatchMessageOptimized(eq(1L), any(Message.class), eq(account));
    }

    @Test
    void forwardMessage_shouldForwardToTargets() throws Exception {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(2L, 3L));
        when(messageHelper.normalizeTargetChatRoomIds(req)).thenReturn(List.of(2L, 3L));

        ChatRoom sourceRoom = new ChatRoom(); sourceRoom.setRoomId(1L);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(sourceRoom));
        when(messageHelper.isUserInChatRoom(1L, account.getAccountId())).thenReturn(true);

        Message sourceMessage = new Message(); sourceMessage.setChatRoomId("1"); sourceMessage.setMessageId("msrc");
        when(messageRepository.findByChatRoomIdAndMessageId("1", "msrc")).thenReturn(Optional.of(sourceMessage));
        // no-op: messageHelper.validateForwardableSourceMessage will be a no-op on the mock

        // targets exist and user is member
        when(chatRoomRepository.findById(2L)).thenReturn(Optional.of(new ChatRoom()));
        when(chatRoomRepository.findById(3L)).thenReturn(Optional.of(new ChatRoom()));
        when(messageHelper.isUserInChatRoom(2L, account.getAccountId())).thenReturn(true);
        when(messageHelper.isUserInChatRoom(3L, account.getAccountId())).thenReturn(true);

        Message f2 = new Message(); f2.setMessageId("f2"); f2.setChatRoomId("2");
        Message f3 = new Message(); f3.setMessageId("f3"); f3.setChatRoomId("3");
        when(messageHelper.createForwardedMessage(sourceMessage, 2L, account)).thenReturn(f2);
        when(messageHelper.createForwardedMessage(sourceMessage, 3L, account)).thenReturn(f3);

        iuh.fit.goat.dto.response.message.MessageResponse resp2 = new iuh.fit.goat.dto.response.message.MessageResponse();
        resp2.setMessageId("f2");
        iuh.fit.goat.dto.response.message.MessageResponse resp3 = new iuh.fit.goat.dto.response.message.MessageResponse();
        resp3.setMessageId("f3");
        when(messageHelper.toMessageResponse(f2)).thenReturn(resp2);
        when(messageHelper.toMessageResponse(f3)).thenReturn(resp3);
        when(messageHelper.saveAndDispatchMessageOptimized(eq(2L), eq(f2), eq(account))).thenReturn(f2);
        when(messageHelper.saveAndDispatchMessageOptimized(eq(3L), eq(f3), eq(account))).thenReturn(f3);

        var resp = messageServiceImpl.forwardMessage(1L, "msrc", req, account);

        assertThat(resp).isNotNull();
        assertThat(resp.getSuccessCount()).isEqualTo(2);
        assertThat(resp.getFailedCount()).isEqualTo(0);
    }

    @Test
    void sendMessagesWithFiles_shouldThrowWhenNoContentOrFiles() throws Exception {
        MessageCreateRequest req = new MessageCreateRequest(null, null);
        ChatRoom chatRoom = new ChatRoom(); chatRoom.setRoomId(1L);
        ChatMember m = new ChatMember(); m.setAccount(account); m.setRole(ChatRole.MEMBER);

        when(messageHelper.getChatRoom(1L)).thenReturn(chatRoom);
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, account.getAccountId())).thenReturn(m);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(any(), any(), any());
        when(messageHelper.normalizeMessageContent(null)).thenReturn(null);

        org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
                messageServiceImpl.sendMessagesWithFiles(1L, req, List.of(), account)
        );
    }

    @Test
    void sendMessage_permissionDenied_throws() throws Exception {
        MessageCreateRequest req = new MessageCreateRequest("hi", null);
        ChatRoom chatRoom = new ChatRoom(); chatRoom.setRoomId(1L);
        when(messageHelper.getChatRoom(1L)).thenReturn(chatRoom);
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, account.getAccountId())).thenReturn(new ChatMember());
        doThrow(new iuh.fit.goat.exception.InvalidException("nope")).when(chatRoomPermissionGuard).assertCanPerformAction(any(), any(), any());

        org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.InvalidException.class, () ->
            messageServiceImpl.sendMessage(1L, req, account)
        );
    }

    @Test
    void sendMessage_blockedInteraction_throws() throws Exception {
        MessageCreateRequest req = new MessageCreateRequest("hi", null);
        ChatRoom chatRoom = new ChatRoom(); chatRoom.setRoomId(1L);
        ChatMember m = new ChatMember(); m.setAccount(account); m.setRole(ChatRole.MEMBER);

        when(messageHelper.getChatRoom(1L)).thenReturn(chatRoom);
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, account.getAccountId())).thenReturn(m);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(any(), any(), any());
        doThrow(new iuh.fit.goat.exception.BlockedInteractionException("blocked")).when(messageHelper).validateNoBlockedDirectInteractionOptimized(eq(chatRoom), any(), eq(account.getAccountId()));

        org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.BlockedInteractionException.class, () ->
                messageServiceImpl.sendMessage(1L, req, account)
        );
    }

    @Test
    void forwardMessage_sourceMissing_throwsNotFound() {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(2L));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(new ChatRoom()));
        when(messageHelper.isUserInChatRoom(1L, account.getAccountId())).thenReturn(true);
        when(messageRepository.findByChatRoomIdAndMessageId("1","msrc")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(iuh.fit.goat.exception.NotFoundException.class, () ->
                messageServiceImpl.forwardMessage(1L, "msrc", req, account)
        );
    }

    @Test
    void forwardMessage_targetNotMember_countsAsFailure() throws Exception {
        ForwardMessageRequest req = new ForwardMessageRequest(List.of(2L));
        when(messageHelper.normalizeTargetChatRoomIds(req)).thenReturn(List.of(2L));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(new ChatRoom()));
        when(messageHelper.isUserInChatRoom(1L, account.getAccountId())).thenReturn(true);
        Message sourceMessage = new Message(); sourceMessage.setChatRoomId("1"); sourceMessage.setMessageId("msrc");
        when(messageRepository.findByChatRoomIdAndMessageId("1","msrc")).thenReturn(Optional.of(sourceMessage));
        lenient().doNothing().when(messageHelper).validateForwardableSourceMessage(any(Message.class));
        when(chatRoomRepository.findById(2L)).thenReturn(Optional.of(new ChatRoom()));
        when(messageHelper.isUserInChatRoom(2L, account.getAccountId())).thenReturn(false);

        var resp = messageServiceImpl.forwardMessage(1L, "msrc", req, account);
        assertThat(resp.getFailedCount()).isEqualTo(1);
    }
}
