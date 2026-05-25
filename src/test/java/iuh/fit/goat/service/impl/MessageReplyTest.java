package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.entity.embeddable.SenderInfo;
import iuh.fit.goat.enumeration.ChatRole;
import iuh.fit.goat.enumeration.ChatRoomType;
import iuh.fit.goat.exception.BlockedInteractionException;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.MessageHiddenRepository;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.AiService;
import iuh.fit.goat.service.StorageService;
import iuh.fit.goat.service.cache.ChatRoomCacheService;
import iuh.fit.goat.service.helper.ChatRoomPermissionGuard;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageReplyTest {

    @Mock private StorageService storageService;
    @Mock private AiService aiService;
    @Mock private MessageHiddenRepository messageHiddenRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChatRoomCacheService chatRoomCache;
    @Mock private MessageHelper messageHelper;
    @Mock private ChatRoomPermissionGuard chatRoomPermissionGuard;

    @InjectMocks private MessageServiceImpl messageService;

    private User currentAccount;
    private ChatRoom groupRoom;
    private ChatRoom directRoom;
    private ChatMember member;
    private SenderInfo senderInfo;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setRoleId(1L);
        role.setName("USER");

        currentAccount = new User();
        currentAccount.setAccountId(10L);
        currentAccount.setEmail("sender@example.com");
        currentAccount.setUsername("sender");
        currentAccount.setFullName("Sender One");
        currentAccount.setPassword("hash");
        currentAccount.setRole(role);

        senderInfo = SenderInfo.builder()
                .accountId(10L)
                .fullName("Sender One")
                .username("sender")
                .email("sender@example.com")
                .avatar("avatar.png")
                .build();

        member = new ChatMember();
        member.setAccount(currentAccount);
        member.setRole(ChatRole.MEMBER);

        groupRoom = new ChatRoom();
        groupRoom.setRoomId(1L);
        groupRoom.setType(ChatRoomType.GROUP);
        groupRoom.setAllowMemberSendMessage(true);
        groupRoom.setAllowModeratorSendMessage(true);

        directRoom = new ChatRoom();
        directRoom.setRoomId(2L);
        directRoom.setType(ChatRoomType.DIRECT);
        directRoom.setAllowMemberSendMessage(true);
        directRoom.setAllowModeratorSendMessage(true);
    }

    @Test
    void sendMessage_nullChatRoomId_throwsInvalid() throws Exception {
        doThrow(new InvalidException("Chat Room not found")).when(messageHelper).validateChatRoom(null);

        assertThrows(InvalidException.class, () ->
                messageService.sendMessage(null, new MessageCreateRequest("hello", null), currentAccount)
        );
        verifyNoInteractions(messageRepository, messageHiddenRepository);
    }

    @Test
    void sendMessage_missingChatRoom_throwsInvalid() throws Exception {
        doThrow(new InvalidException("Chat Room not found")).when(messageHelper).validateChatRoom(99L);

        assertThrows(InvalidException.class, () ->
                messageService.sendMessage(99L, new MessageCreateRequest("hello", null), currentAccount)
        );
    }

    @Test
    void sendMessage_currentMemberMissing_throwsInvalid() throws Exception {
        when(messageHelper.getChatRoom(1L)).thenReturn(groupRoom);
        when(chatRoomPermissionGuard.getCurrentMember(groupRoom, currentAccount.getAccountId()))
                .thenThrow(new InvalidException("User is not a member of this chat room"));

        assertThrows(InvalidException.class, () ->
                messageService.sendMessage(1L, new MessageCreateRequest("hello", null), currentAccount)
        );
    }

    @Test
    void sendMessage_permissionDenied_throwsInvalid() throws Exception {
        when(messageHelper.getChatRoom(1L)).thenReturn(groupRoom);
        when(chatRoomPermissionGuard.getCurrentMember(groupRoom, currentAccount.getAccountId())).thenReturn(member);
        doThrow(new InvalidException("nope")).when(chatRoomPermissionGuard).assertCanPerformAction(any(), any(), any());

        assertThrows(InvalidException.class, () ->
                messageService.sendMessage(1L, new MessageCreateRequest("hello", null), currentAccount)
        );
    }

    @Test
    void sendMessage_blockedDirectInteraction_throwsBlockedInteraction() throws Exception {
        when(messageHelper.getChatRoom(2L)).thenReturn(directRoom);
        when(chatRoomPermissionGuard.getCurrentMember(directRoom, currentAccount.getAccountId())).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(any(), any(), any());
        doThrow(new BlockedInteractionException("blocked")).when(messageHelper)
                .validateNoBlockedDirectInteractionOptimized(eq(directRoom), any(), eq(currentAccount.getAccountId()));

        assertThrows(BlockedInteractionException.class, () ->
                messageService.sendMessage(2L, new MessageCreateRequest("hello", null), currentAccount)
        );
    }

    @Test
    void sendMessage_nullRequest_throwsNullPointerException() throws Exception {
        when(messageHelper.getChatRoom(1L)).thenReturn(groupRoom);
        when(chatRoomPermissionGuard.getCurrentMember(groupRoom, currentAccount.getAccountId())).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(any(), any(), any());
        doNothing().when(messageHelper).validateNoBlockedDirectInteractionOptimized(any(), any(), any());
        doNothing().when(messageHelper).validateReplyTarget(eq("1"), eq(null));

        assertThrows(NullPointerException.class, () ->
                messageService.sendMessage(1L, null, currentAccount)
        );
    }

    @Test
    void sendMessage_blankReplyId_isNormalizedToNull() throws Exception {
        when(messageHelper.getChatRoom(1L)).thenReturn(groupRoom);
        when(chatRoomPermissionGuard.getCurrentMember(groupRoom, currentAccount.getAccountId())).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(any(), any(), any());
        doNothing().when(messageHelper).validateNoBlockedDirectInteractionOptimized(any(), any(), any());
        when(messageHelper.normalizeReplyToMessageId("   ")).thenReturn(null);
        doNothing().when(messageHelper).validateReplyTarget("1", null);
        when(messageHelper.generateMessageId()).thenReturn("msg_1");
        when(messageHelper.buildSenderInfo(currentAccount)).thenReturn(senderInfo);
        when(messageHelper.saveAndDispatchMessageOptimized(eq(1L), any(Message.class), eq(currentAccount)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        Message created = messageService.sendMessage(1L, new MessageCreateRequest("hello", "   "), currentAccount);

        assertThat(created.getReplyTo()).isNull();
    }

    @Test
    void sendMessage_replyIdIsTrimmedBeforeValidation() throws Exception {
        when(messageHelper.getChatRoom(1L)).thenReturn(groupRoom);
        when(chatRoomPermissionGuard.getCurrentMember(groupRoom, currentAccount.getAccountId())).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(any(), any(), any());
        doNothing().when(messageHelper).validateNoBlockedDirectInteractionOptimized(any(), any(), any());
        when(messageHelper.normalizeReplyToMessageId("  reply-1  ")).thenReturn("reply-1");
        doNothing().when(messageHelper).validateReplyTarget("1", "reply-1");
        when(messageHelper.generateMessageId()).thenReturn("msg_2");
        when(messageHelper.buildSenderInfo(currentAccount)).thenReturn(senderInfo);
        when(messageHelper.saveAndDispatchMessageOptimized(eq(1L), any(Message.class), eq(currentAccount)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        Message created = messageService.sendMessage(1L, new MessageCreateRequest("hello", "  reply-1  "), currentAccount);

        assertThat(created.getReplyTo()).isEqualTo("reply-1");
        verify(messageHelper).validateReplyTarget("1", "reply-1");
    }

    @Test
    void sendMessage_invalidReplyTarget_throwsInvalid() throws Exception {
        when(messageHelper.getChatRoom(1L)).thenReturn(groupRoom);
        when(chatRoomPermissionGuard.getCurrentMember(groupRoom, currentAccount.getAccountId())).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(any(), any(), any());
        doNothing().when(messageHelper).validateNoBlockedDirectInteractionOptimized(any(), any(), any());
        when(messageHelper.normalizeReplyToMessageId("bad-reply")).thenReturn("bad-reply");
        doThrow(new InvalidException("replyToMessageId is invalid or not in this conversation"))
                .when(messageHelper).validateReplyTarget("1", "bad-reply");

        assertThrows(InvalidException.class, () ->
                messageService.sendMessage(1L, new MessageCreateRequest("hello", "bad-reply"), currentAccount)
        );
    }

    @Test
    void sendMessage_replyTargetOnSourceRoom_isSaved() throws Exception {
        when(messageHelper.getChatRoom(1L)).thenReturn(groupRoom);
        when(chatRoomPermissionGuard.getCurrentMember(groupRoom, currentAccount.getAccountId())).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(any(), any(), any());
        doNothing().when(messageHelper).validateNoBlockedDirectInteractionOptimized(any(), any(), any());
        when(messageHelper.normalizeReplyToMessageId("reply-1")).thenReturn("reply-1");
        doNothing().when(messageHelper).validateReplyTarget("1", "reply-1");
        when(messageHelper.generateMessageId()).thenReturn("msg_3");
        when(messageHelper.buildSenderInfo(currentAccount)).thenReturn(senderInfo);
        when(messageHelper.saveAndDispatchMessageOptimized(eq(1L), any(Message.class), eq(currentAccount)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        Message created = messageService.sendMessage(1L, new MessageCreateRequest("hello", "reply-1"), currentAccount);

        assertThat(created.getReplyTo()).isEqualTo("reply-1");
        assertThat(created.getContent()).isEqualTo("hello");
    }

    @Test
    void sendMessage_replyTargetValidationUsesCurrentChatRoomId() throws Exception {
        when(messageHelper.getChatRoom(2L)).thenReturn(directRoom);
        when(chatRoomPermissionGuard.getCurrentMember(directRoom, currentAccount.getAccountId())).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(any(), any(), any());
        doNothing().when(messageHelper).validateNoBlockedDirectInteractionOptimized(any(), any(), any());
        when(messageHelper.normalizeReplyToMessageId("reply-77")).thenReturn("reply-77");
        doNothing().when(messageHelper).validateReplyTarget("2", "reply-77");
        when(messageHelper.generateMessageId()).thenReturn("msg_4");
        when(messageHelper.buildSenderInfo(currentAccount)).thenReturn(senderInfo);
        when(messageHelper.saveAndDispatchMessageOptimized(eq(2L), any(Message.class), eq(currentAccount)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        Message created = messageService.sendMessage(2L, new MessageCreateRequest("hello", "reply-77"), currentAccount);

        assertThat(created.getChatRoomId()).isEqualTo("2");
        verify(messageHelper).validateReplyTarget("2", "reply-77");
    }

    @Test
    void sendMessage_success_withReplyCapturesReplyField() throws Exception {
        when(messageHelper.getChatRoom(1L)).thenReturn(groupRoom);
        when(chatRoomPermissionGuard.getCurrentMember(groupRoom, currentAccount.getAccountId())).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(any(), any(), any());
        doNothing().when(messageHelper).validateNoBlockedDirectInteractionOptimized(any(), any(), any());
        when(messageHelper.normalizeReplyToMessageId("reply-9")).thenReturn("reply-9");
        doNothing().when(messageHelper).validateReplyTarget("1", "reply-9");
        when(messageHelper.generateMessageId()).thenReturn("msg_5");
        when(messageHelper.buildSenderInfo(currentAccount)).thenReturn(senderInfo);
        when(messageHelper.saveAndDispatchMessageOptimized(eq(1L), any(Message.class), eq(currentAccount)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        when(messageHelper.saveAndDispatchMessageOptimized(eq(1L), captor.capture(), eq(currentAccount)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        Message created = messageService.sendMessage(1L, new MessageCreateRequest("hello", "reply-9"), currentAccount);

        assertThat(created.getReplyTo()).isEqualTo("reply-9");
        assertThat(captor.getValue().getReplyTo()).isEqualTo("reply-9");
        assertThat(captor.getValue().getIsHidden()).isFalse();
        assertThat(captor.getValue().getIsForwarded()).isFalse();
    }
}