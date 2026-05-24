package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.chat.ChatRoomResponse;
import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.ChatRoomType;
import iuh.fit.goat.repository.ChatMemberRepository;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.UserRelationshipRepository;
import iuh.fit.goat.service.AiService;
import iuh.fit.goat.service.MessageService;
import iuh.fit.goat.service.NotificationService;
import iuh.fit.goat.service.cache.ChatRoomCacheService;
import iuh.fit.goat.service.helper.ChatRoomPermissionGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceImplUnitTest {

    @Mock private MessageService messageService;
    @Mock private NotificationService notificationService;
    @Mock private AiService aiService;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private UserRelationshipRepository userRelationshipRepository;
    @Mock private ChatRoomCacheService chatRoomCacheService;
    @Mock private ChatRoomPermissionGuard chatRoomPermissionGuard;

    @InjectMocks private ChatRoomServiceImpl chatRoomService;

    @BeforeEach
    void setUp() {
        // no-op
    }

    @Test
    void existsDirectChatRoom_returnsNullOrLatest() {
        // empty
        when(chatRoomRepository.findDirectChatRoomsBetweenUsersOrderByLatest(1L, 2L)).thenReturn(List.of());
        var none = chatRoomService.existsDirectChatRoom(1L, 2L);
        assertThat(none).isNull();

        // single
        ChatRoom one = new ChatRoom(); one.setRoomId(10L); one.setType(ChatRoomType.DIRECT);
        when(chatRoomRepository.findDirectChatRoomsBetweenUsersOrderByLatest(1L, 3L)).thenReturn(List.of(one));
        var single = chatRoomService.existsDirectChatRoom(1L, 3L);
        assertThat(single).isNotNull();
        assertThat(single.getRoomId()).isEqualTo(10L);

        // multiple -> returns latest (first)
        ChatRoom a = new ChatRoom(); a.setRoomId(21L); a.setType(ChatRoomType.DIRECT);
        ChatRoom b = new ChatRoom(); b.setRoomId(22L); b.setType(ChatRoomType.DIRECT);
        when(chatRoomRepository.findDirectChatRoomsBetweenUsersOrderByLatest(4L, 5L)).thenReturn(List.of(a, b));
        var latest = chatRoomService.existsDirectChatRoom(4L, 5L);
        assertThat(latest).isNotNull();
        assertThat(latest.getRoomId()).isEqualTo(21L);
    }

    @Test
    void getDetailChatRoomInformation_generatesGroupNameForThreeAndManyMembers() throws Exception {
        ChatRoom group = new ChatRoom();
        group.setRoomId(100L);
        group.setType(ChatRoomType.GROUP);
        // prepare 3 members
        List<ChatMember> members = new ArrayList<>();
        User u1 = new User(); u1.setAccountId(1L); u1.setUsername("u1"); u1.setFullName("User One");
        User u2 = new User(); u2.setAccountId(2L); u2.setUsername("u2"); u2.setFullName("User Two");
        User u3 = new User(); u3.setAccountId(3L); u3.setUsername("u3"); u3.setFullName("User Three");
        ChatMember m1 = new ChatMember(); m1.setAccount(u1); m1.setMemberId(11L);
        ChatMember m2 = new ChatMember(); m2.setAccount(u2); m2.setMemberId(12L);
        ChatMember m3 = new ChatMember(); m3.setAccount(u3); m3.setMemberId(13L);
        members.add(m1); members.add(m2); members.add(m3);
        group.setMembers(members);

        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(group));
        // permission guard returns current member so getDetailChatRoomInformation proceeds
        when(chatRoomPermissionGuard.getCurrentMember(group, 1L)).thenReturn(m1);

        ChatRoomResponse resp = chatRoomService.getDetailChatRoomInformation(u1, 100L);
        // For 3 members, name should be "u1, u2 và u3" (usernames used when fullName present)
        assertThat(resp.getName()).contains("u1, u2");

        // now test >3 members pattern
        User u4 = new User(); u4.setAccountId(4L); u4.setUsername("u4"); u4.setFullName("User Four");
        ChatMember m4 = new ChatMember(); m4.setAccount(u4); m4.setMemberId(14L);
        group.setMembers(List.of(m1, m2, m3, m4));
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(group));

        ChatRoomResponse resp2 = chatRoomService.getDetailChatRoomInformation(u1, 100L);
        assertThat(resp2.getName()).contains("và (2) người khác");
    }
}
