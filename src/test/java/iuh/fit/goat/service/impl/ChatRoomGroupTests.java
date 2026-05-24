package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.chat.CreateGroupChatRequest;
import iuh.fit.goat.dto.request.chat.UpdateChatRoomPermissionsRequest;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.ChatRole;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.ChatMemberRepository;
import iuh.fit.goat.repository.ChatRoomJoinRequestRepository;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import iuh.fit.goat.enumeration.ChatRoomType;
import iuh.fit.goat.enumeration.ChatRoomPrivacy;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatRoomGroupTests {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private ChatRoomJoinRequestRepository joinRequestRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private MessageService messageService;

    @InjectMocks private ChatRoomServiceImpl chatRoomService;

    private Account owner;

    @BeforeEach
    void setUp() {
        owner = new User(); owner.setAccountId(500L);
    }

    @Test
    void createGroupChat_shouldCreateAndSendSystemMessage() throws Exception {
        CreateGroupChatRequest req = new CreateGroupChatRequest(List.of(500L, 501L), "Group", null);
        when(accountRepository.findAllByAccountIdInAndDeletedAtIsNull(List.of(500L,501L))).thenReturn(List.of(new User(), new User()));
        when(chatRoomRepository.saveAndFlush(any(ChatRoom.class))).thenAnswer(i -> i.getArgument(0));
        when(chatMemberRepository.saveAllAndFlush(any())).thenReturn(null);

        var room = chatRoomService.createGroupChat(owner, req);
        assertThat(room).isNotNull();
        verify(messageService).createAndSendSystemMessage(eq(room.getRoomId()), any(), eq(owner), any());
    }

    @Test
    void updateGroupPermissions_moderatorCannotChangeModeratorFlag() {
        UpdateChatRoomPermissionsRequest req = new UpdateChatRoomPermissionsRequest(true, true, true, true, true);
        ChatRoom room = new ChatRoom(); room.setRoomId(77L); room.setMembers(List.of());
        ChatMember moderator = new ChatMember(); moderator.setAccount(owner); moderator.setRole(ChatRole.MODERATOR);
        room.setMembers(List.of(moderator));
        when(chatRoomRepository.findByRoomId(77L)).thenReturn(Optional.of(room));

        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                chatRoomService.updateGroupPermissions(owner, 77L, req)
        );
    }

    @Test
    void joinByInvite_publicRoom_shouldJoin() throws Exception {
        ChatRoom room = new ChatRoom(); room.setRoomId(123L); room.setInviteEnabled(true); room.setPrivacy(null);
        room.setType(ChatRoomType.GROUP);
        when(chatRoomRepository.findByInviteTokenAndDeletedAtIsNull("token")).thenReturn(Optional.of(room));
        when(chatMemberRepository.existsByRoomRoomIdAndAccountAccountIdAndDeletedAtIsNull(123L, 600L)).thenReturn(false);
        var user = new User(); user.setAccountId(600L);

        var resp = chatRoomService.joinByInvite(user, "token");
        assertThat(resp.isJoined()).isTrue();
    }
}
