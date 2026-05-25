package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.message.MessageReactionResponse;
import iuh.fit.goat.dto.response.message.ReactionUpdateEvent;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.entity.UserReaction;
import iuh.fit.goat.enumeration.ChatRole;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.MessageRepository;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageReactionTest {

    @Mock private MessageRepository messageRepository;
    @Mock private MessageHelper messageHelper;
    @Mock private ChatRoomPermissionGuard permissionGuard;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private MessageReactionServiceImpl messageReactionService;

    private ChatRoom chatRoom;
    private ChatMember member;
    private User user;
    private Company company;

    @BeforeEach
    void setUp() {
        chatRoom = new ChatRoom();
        chatRoom.setRoomId(1L);

        user = new User();
        user.setAccountId(10L);
        user.setUsername("user1");
        user.setFullName("User One");
        user.setAvatar("user.png");

        company = new Company();
        company.setAccountId(20L);
        company.setUsername("company1");
        company.setName("Acme Corp");
        company.setAvatar("company.png");

        member = new ChatMember();
        member.setAccount(user);
        member.setRole(ChatRole.MEMBER);
    }

    @Test
    void addReaction_nullEmoji_throwsInvalid() throws Exception {
        assertThrows(InvalidException.class, () -> messageReactionService.addReaction(1L, "m1", user, null));
        verifyNoInteractions(messageHelper, permissionGuard, messageRepository, messagingTemplate);
    }

    @Test
    void addReaction_blankEmoji_throwsInvalid() throws Exception {
        assertThrows(InvalidException.class, () -> messageReactionService.addReaction(1L, "m1", user, " "));
        verifyNoInteractions(messageHelper, permissionGuard, messageRepository, messagingTemplate);
    }

    @Test
    void addReaction_invalidEmoji_throwsInvalid() throws Exception {
        assertThrows(InvalidException.class, () -> messageReactionService.addReaction(1L, "m1", user, "A"));
        verifyNoInteractions(messageHelper, permissionGuard, messageRepository, messagingTemplate);
    }

    @Test
    void addReaction_messageNotFound_throwsInvalid() throws Exception {
        when(messageHelper.getChatRoom(1L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, user.getAccountId())).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m1")).thenReturn(Optional.empty());

        assertThrows(InvalidException.class, () -> messageReactionService.addReaction(1L, "m1", user, "👍"));
    }

    @Test
    void addReaction_hiddenMessage_throwsInvalid() throws Exception {
        Message message = new Message();
        message.setChatRoomId("1");
        message.setMessageId("m1");
        message.setIsHidden(true);

        when(messageHelper.getChatRoom(1L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, user.getAccountId())).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m1")).thenReturn(Optional.of(message));

        assertThrows(InvalidException.class, () -> messageReactionService.addReaction(1L, "m1", user, "👍"));
    }

    @Test
    void addReaction_success_addsFirstReaction_andBroadcasts() throws Exception {
        Message message = new Message();
        message.setChatRoomId("1");
        message.setMessageId("m1");
        message.setReactions(new HashMap<>());

        when(messageHelper.getChatRoom(1L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, user.getAccountId())).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m1")).thenReturn(Optional.of(message));
        when(messageRepository.saveMessage(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageReactionResponse response = messageReactionService.addReaction(1L, "m1", user, "👍");

        assertThat(response.getMessageId()).isEqualTo("m1");
        assertThat(response.getReactions()).hasSize(1);
        assertThat(response.getReactions().getFirst().getEmoji()).isEqualTo("👍");
        assertThat(response.getReactions().getFirst().getCount()).isEqualTo(1);

        ArgumentCaptor<ReactionUpdateEvent> eventCaptor = ArgumentCaptor.forClass(ReactionUpdateEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chatrooms/1/reactions"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getAction()).isEqualTo("ADDED");
        assertThat(eventCaptor.getValue().getEmoji()).isEqualTo("👍");
    }

    @Test
    void addReaction_success_usesCompanyNameForReactionUser() throws Exception {
        Message message = new Message();
        message.setChatRoomId("1");
        message.setMessageId("m1");
        message.setReactions(new HashMap<>());

        when(messageHelper.getChatRoom(1L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, company.getAccountId())).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m1")).thenReturn(Optional.of(message));
        when(messageRepository.saveMessage(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageReactionResponse response = messageReactionService.addReaction(1L, "m1", company, "👍");

        assertThat(response.getReactions().getFirst().getUsers().getFirst().getFullName()).isEqualTo("Acme Corp");
    }

    @Test
    void addReaction_sameEmojiFromSameUser_removesReaction() throws Exception {
        UserReaction reaction = new UserReaction();
        reaction.setAccountId(user.getAccountId());
        reaction.setFullName(user.getFullName());
        reaction.setUsername(user.getUsername());

        Map<String, List<UserReaction>> reactions = new HashMap<>();
        reactions.put("👍", new ArrayList<>(List.of(reaction)));

        Message message = new Message();
        message.setChatRoomId("1");
        message.setMessageId("m1");
        message.setReactions(reactions);

        when(messageHelper.getChatRoom(1L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, user.getAccountId())).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m1")).thenReturn(Optional.of(message));
        when(messageRepository.saveMessage(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageReactionResponse response = messageReactionService.addReaction(1L, "m1", user, "👍");

        assertThat(response.getReactions()).isEmpty();
        verify(messagingTemplate).convertAndSend(eq("/topic/chatrooms/1/reactions"), any(ReactionUpdateEvent.class));
    }

    @Test
    void addReaction_differentEmojiFromSameUser_replacesPreviousEmoji() throws Exception {
        UserReaction reaction = new UserReaction();
        reaction.setAccountId(user.getAccountId());
        reaction.setFullName(user.getFullName());
        reaction.setUsername(user.getUsername());

        Map<String, List<UserReaction>> reactions = new HashMap<>();
        reactions.put("👍", new ArrayList<>(List.of(reaction)));

        Message message = new Message();
        message.setChatRoomId("1");
        message.setMessageId("m1");
        message.setReactions(reactions);

        when(messageHelper.getChatRoom(1L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, user.getAccountId())).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m1")).thenReturn(Optional.of(message));
        when(messageRepository.saveMessage(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageReactionResponse response = messageReactionService.addReaction(1L, "m1", user, "😂");

        assertThat(response.getReactions()).hasSize(1);
        assertThat(response.getReactions().getFirst().getEmoji()).isEqualTo("😂");
        verify(messagingTemplate).convertAndSend(eq("/topic/chatrooms/1/reactions"), any(ReactionUpdateEvent.class));
    }

    @Test
    void addReaction_reactionsNull_initializesMap() throws Exception {
        Message message = new Message();
        message.setChatRoomId("1");
        message.setMessageId("m1");
        message.setReactions(null);

        when(messageHelper.getChatRoom(1L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, user.getAccountId())).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m1")).thenReturn(Optional.of(message));
        when(messageRepository.saveMessage(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageReactionResponse response = messageReactionService.addReaction(1L, "m1", user, "😂");

        assertThat(response.getReactions()).hasSize(1);
        assertThat(message.getReactions()).containsKey("😂");
    }

    @Test
    void removeReaction_invalidEmoji_throwsInvalid() throws Exception {
        assertThrows(InvalidException.class, () -> messageReactionService.removeReaction(1L, "m1", user, "x"));
        verifyNoInteractions(messageHelper, permissionGuard, messageRepository, messagingTemplate);
    }

    @Test
    void removeReaction_noReactions_returnsEmptyWithoutSaving() throws Exception {
        Message message = new Message();
        message.setChatRoomId("1");
        message.setMessageId("m1");
        message.setReactions(null);

        when(messageHelper.getChatRoom(1L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, user.getAccountId())).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m1")).thenReturn(Optional.of(message));

        MessageReactionResponse response = messageReactionService.removeReaction(1L, "m1", user, "👍");

        assertThat(response.getReactions()).isEmpty();
        verify(messageRepository, never()).saveMessage(any(Message.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(ReactionUpdateEvent.class));
    }

    @Test
    void removeReaction_userDidNotReact_returnsWithoutSaving() throws Exception {
        UserReaction other = new UserReaction();
        other.setAccountId(99L);
        other.setFullName("Other");

        Map<String, List<UserReaction>> reactions = new HashMap<>();
        reactions.put("👍", new ArrayList<>(List.of(other)));

        Message message = new Message();
        message.setChatRoomId("1");
        message.setMessageId("m1");
        message.setReactions(reactions);

        when(messageHelper.getChatRoom(1L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, user.getAccountId())).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m1")).thenReturn(Optional.of(message));

        MessageReactionResponse response = messageReactionService.removeReaction(1L, "m1", user, "👍");

        assertThat(response.getReactions()).hasSize(1);
        verify(messageRepository, never()).saveMessage(any(Message.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(ReactionUpdateEvent.class));
    }

    @Test
    void removeReaction_success_removesAndBroadcasts() throws Exception {
        UserReaction reaction = new UserReaction();
        reaction.setAccountId(user.getAccountId());
        reaction.setFullName(user.getFullName());
        reaction.setUsername(user.getUsername());

        Map<String, List<UserReaction>> reactions = new HashMap<>();
        reactions.put("👍", new ArrayList<>(List.of(reaction)));

        Message message = new Message();
        message.setChatRoomId("1");
        message.setMessageId("m1");
        message.setReactions(reactions);

        when(messageHelper.getChatRoom(1L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, user.getAccountId())).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m1")).thenReturn(Optional.of(message));
        when(messageRepository.saveMessage(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageReactionResponse response = messageReactionService.removeReaction(1L, "m1", user, "👍");

        assertThat(response.getReactions()).isEmpty();
        ArgumentCaptor<ReactionUpdateEvent> eventCaptor = ArgumentCaptor.forClass(ReactionUpdateEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chatrooms/1/reactions"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getAction()).isEqualTo("REMOVED");
        assertThat(eventCaptor.getValue().getEmoji()).isEqualTo("👍");
    }

    @Test
    void getReactions_returnsSortedGroups() throws Exception {
        UserReaction u1 = new UserReaction();
        u1.setAccountId(10L);
        u1.setFullName("User One");
        UserReaction u2 = new UserReaction();
        u2.setAccountId(11L);
        u2.setFullName("User Two");

        Map<String, List<UserReaction>> reactions = new HashMap<>();
        reactions.put("👍", new ArrayList<>(List.of(u1, u2)));
        reactions.put("❤️", new ArrayList<>(List.of(u1)));

        Message message = new Message();
        message.setChatRoomId("1");
        message.setMessageId("m1");
        message.setReactions(reactions);

        when(messageHelper.getChatRoom(1L)).thenReturn(chatRoom);
        when(permissionGuard.getCurrentMember(chatRoom, user.getAccountId())).thenReturn(member);
        when(messageRepository.findByChatRoomIdAndMessageId("1", "m1")).thenReturn(Optional.of(message));

        MessageReactionResponse response = messageReactionService.getReactions(1L, "m1", user);

        assertThat(response.getReactions()).hasSize(2);
        assertThat(response.getReactions().getFirst().getEmoji()).isEqualTo("👍");
        assertThat(response.getReactions().getFirst().getCount()).isEqualTo(2);
        assertThat(response.getReactions().get(1).getEmoji()).isEqualTo("❤️");
        assertThat(response.getReactions().get(1).getCount()).isEqualTo(1);
    }
}