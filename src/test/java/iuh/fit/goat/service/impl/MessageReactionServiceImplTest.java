package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.message.MessageReactionResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.service.helper.ChatRoomPermissionGuard;
import iuh.fit.goat.service.helper.MessageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import iuh.fit.goat.exception.InvalidException;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class MessageReactionServiceImplTest {

    @Mock private MessageRepository messageRepository;
    @Mock private MessageHelper messageHelper;
    @Mock private ChatRoomPermissionGuard permissionGuard;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageReactionServiceImpl messageReactionService;

    private User currentUser;
    private ChatRoom chatRoom;
    private ChatMember member;
    private Message message;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setAccountId(801L);
        currentUser.setEmail("reactor@example.com");
        currentUser.setUsername("reactor");
        currentUser.setFullName("Reactor One");

        chatRoom = new ChatRoom();
        chatRoom.setRoomId(55L);

        member = new ChatMember();
        member.setAccount(currentUser);

        message = new Message();
        message.setChatRoomId("55");
        message.setMessageId("m1");
        message.setIsHidden(false);
    }

    @Test
    void addReaction_shouldPersistReaction() throws Exception {
        when(messageHelper.getChatRoom(55L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, 801L)).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("55", "m1")).thenReturn(Optional.of(message));

        MessageReactionResponse response = messageReactionService.addReaction(55L, "m1", currentUser, "👍");

        assertThat(response.getMessageId()).isEqualTo("m1");
        assertThat(response.getReactions()).hasSize(1);
        verify(messageRepository).saveMessage(message);
        verify(messagingTemplate).convertAndSend(eq("/topic/chatrooms/55/reactions"), (Object) any());
    }

    @Test
    void getReactions_shouldReturnEmptyWhenNoneExist() throws Exception {
        when(messageHelper.getChatRoom(55L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, 801L)).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("55", "m1")).thenReturn(Optional.of(message));

        MessageReactionResponse response = messageReactionService.getReactions(55L, "m1", currentUser);

        assertThat(response.getMessageId()).isEqualTo("m1");
        assertThat(response.getReactions()).isEmpty();
    }

    @Test
    void addReaction_invalidEmoji_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageReactionService.addReaction(55L, "m1", currentUser, "notemoji")
        );
    }

    @Test
    void addReaction_messageNotFound_throws() throws Exception {
        when(messageHelper.getChatRoom(55L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, 801L)).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("55", "m1")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageReactionService.addReaction(55L, "m1", currentUser, "👍")
        );
    }

    @Test
    void addReaction_messageHidden_throws() throws Exception {
        message.setIsHidden(true);
        when(messageHelper.getChatRoom(55L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, 801L)).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("55", "m1")).thenReturn(Optional.of(message));

        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageReactionService.addReaction(55L, "m1", currentUser, "👍")
        );
    }

    @Test
    void addReaction_replaceExistingEmoji_replaced() throws Exception {
        when(messageHelper.getChatRoom(55L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, 801L)).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("55", "m1")).thenReturn(Optional.of(message));

        Map<String, java.util.List<iuh.fit.goat.entity.UserReaction>> reactions = new HashMap<>();
        iuh.fit.goat.entity.UserReaction ur = new iuh.fit.goat.entity.UserReaction(); ur.setAccountId(801L);
        reactions.put("😄", new java.util.ArrayList<>(java.util.List.of(ur)));
        message.setReactions(reactions);

        var resp = messageReactionService.addReaction(55L, "m1", currentUser, "👍");
        assertThat(resp.getReactions()).isNotEmpty();
    }

    @Test
    void removeReaction_invalidEmoji_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageReactionService.removeReaction(55L, "m1", currentUser, "x")
        );
    }

    @Test
    void removeReaction_noReactions_returnsEmpty() throws Exception {
        when(messageHelper.getChatRoom(55L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, 801L)).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("55", "m1")).thenReturn(Optional.of(message));

        var resp = messageReactionService.removeReaction(55L, "m1", currentUser, "👍");
        assertThat(resp.getReactions()).isEmpty();
    }
}