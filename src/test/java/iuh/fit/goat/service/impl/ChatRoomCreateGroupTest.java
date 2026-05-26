package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.MessageEvent;
import iuh.fit.goat.dto.request.chat.CreateGroupChatRequest;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.ChatRole;
import iuh.fit.goat.enumeration.ChatRoomType;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.ChatMemberRepository;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.service.MessageService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatRoomCreateGroupTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private MessageService messageService;
    @Mock private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @InjectMocks private ChatRoomServiceImpl chatRoomService;

    private Account currentAccount;

    @BeforeEach
    void setUp() {
        currentAccount = new User();
        currentAccount.setAccountId(500L);
    }

    @Test
    void createGroup_success_withNameAndAvatar() throws Exception {
        CreateGroupChatRequest req = new CreateGroupChatRequest(List.of(500L, 600L), "Team", "avatar.png");
        Account a1 = new User(); a1.setAccountId(500L);
        Account a2 = new User(); a2.setAccountId(600L);

        when(accountRepository.findAllByAccountIdInAndDeletedAtIsNull(req.getAccountIds())).thenReturn(List.of(a1, a2));
        when(chatRoomRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ChatRoom r = invocation.getArgument(0);
            r.setRoomId(77L);
            return r;
        });

        when(chatMemberRepository.saveAllAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        ChatRoom created = chatRoomService.createGroupChat(currentAccount, req);

        assertThat(created).isNotNull();
        assertThat(created.getRoomId()).isEqualTo(77L);
        assertThat(created.getName()).isEqualTo("Team");
        assertThat(created.getAvatar()).isEqualTo("avatar.png");
        verify(chatMemberRepository).saveAllAndFlush(any());
        verify(messageService).createAndSendSystemMessage(eq(77L), eq(MessageEvent.GROUP_CREATED), eq(currentAccount), eq("Team"));
    }

    @Test
    void createGroup_defaultNameWhenNull() throws Exception {
        CreateGroupChatRequest req = new CreateGroupChatRequest(List.of(500L, 601L), null, null);
        Account a1 = new User(); a1.setAccountId(500L);
        Account a2 = new User(); a2.setAccountId(601L);

        when(accountRepository.findAllByAccountIdInAndDeletedAtIsNull(req.getAccountIds())).thenReturn(List.of(a1, a2));
        when(chatRoomRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ChatRoom r = invocation.getArgument(0);
            r.setRoomId(88L);
            return r;
        });
        when(chatMemberRepository.saveAllAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        ChatRoom created = chatRoomService.createGroupChat(currentAccount, req);

        assertThat(created.getName()).isEqualTo("Nhóm mới");
    }

    @Test
    void createGroup_blankAvatar_notSet() throws Exception {
        CreateGroupChatRequest req = new CreateGroupChatRequest(List.of(500L, 602L), "Name", "   ");
        Account a1 = new User(); a1.setAccountId(500L);
        Account a2 = new User(); a2.setAccountId(602L);

        when(accountRepository.findAllByAccountIdInAndDeletedAtIsNull(req.getAccountIds())).thenReturn(List.of(a1, a2));
        when(chatRoomRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ChatRoom r = invocation.getArgument(0);
            r.setRoomId(99L);
            return r;
        });
        when(chatMemberRepository.saveAllAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        ChatRoom created = chatRoomService.createGroupChat(currentAccount, req);
        assertThat(created.getAvatar()).isNull();
    }

    @Test
    void createGroup_missingAccounts_throwsInvalid() {
        CreateGroupChatRequest req = new CreateGroupChatRequest(List.of(700L, 701L), "X", null);
        when(accountRepository.findAllByAccountIdInAndDeletedAtIsNull(req.getAccountIds())).thenReturn(List.of());

        assertThrows(InvalidException.class, () -> chatRoomService.createGroupChat(currentAccount, req));
    }

    @Test
    void createGroup_duplicateAccountIds_throwsInvalid() {
        CreateGroupChatRequest req = new CreateGroupChatRequest(List.of(800L, 800L), "dup", null);
        Account a = new User(); a.setAccountId(800L);
        // returned accounts size (1) != input size (2)
        when(accountRepository.findAllByAccountIdInAndDeletedAtIsNull(req.getAccountIds())).thenReturn(List.of(a));

        assertThrows(InvalidException.class, () -> chatRoomService.createGroupChat(currentAccount, req));
    }

    @Test
    void createGroup_ownerAndMemberRoles_assignedCorrectly() throws Exception {
        CreateGroupChatRequest req = new CreateGroupChatRequest(List.of(500L, 900L, 901L), "Roles", null);
        Account a1 = new User(); a1.setAccountId(500L);
        Account a2 = new User(); a2.setAccountId(900L);
        Account a3 = new User(); a3.setAccountId(901L);

        when(accountRepository.findAllByAccountIdInAndDeletedAtIsNull(req.getAccountIds())).thenReturn(List.of(a1, a2, a3));
        when(chatRoomRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ChatRoom r = invocation.getArgument(0);
            r.setRoomId(123L);
            return r;
        });

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        when(chatMemberRepository.saveAllAndFlush(captor.capture())).thenAnswer(i -> i.getArgument(0));

        chatRoomService.createGroupChat(currentAccount, req);

        List<ChatMember> saved = captor.getValue();
        assertThat(saved).hasSize(3);
        assertThat(saved).anyMatch(m -> m.getRole() == ChatRole.OWNER);
        assertThat(saved).anyMatch(m -> m.getRole() == ChatRole.MEMBER);
    }

    @Test
    void createGroup_messageService_calledWithGroupName() throws Exception {
        CreateGroupChatRequest req = new CreateGroupChatRequest(List.of(500L, 1000L), "GroupX", null);
        Account a1 = new User(); a1.setAccountId(500L);
        Account a2 = new User(); a2.setAccountId(1000L);

        when(accountRepository.findAllByAccountIdInAndDeletedAtIsNull(req.getAccountIds())).thenReturn(List.of(a1, a2));
        when(chatRoomRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ChatRoom r = invocation.getArgument(0);
            r.setRoomId(444L);
            return r;
        });
        when(chatMemberRepository.saveAllAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        chatRoomService.createGroupChat(currentAccount, req);

        verify(messageService).createAndSendSystemMessage(eq(444L), eq(MessageEvent.GROUP_CREATED), eq(currentAccount), eq("GroupX"));
    }

    @Test
    void createGroup_currentUserNotInInput_stillAddsOwner() throws Exception {
        CreateGroupChatRequest req = new CreateGroupChatRequest(List.of(1100L, 1101L), "NoOwnerInList", null);
        Account a1 = new User(); a1.setAccountId(1100L);
        Account a2 = new User(); a2.setAccountId(1101L);

        when(accountRepository.findAllByAccountIdInAndDeletedAtIsNull(req.getAccountIds())).thenReturn(List.of(a1, a2));
        when(chatRoomRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ChatRoom r = invocation.getArgument(0);
            r.setRoomId(555L);
            return r;
        });

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        when(chatMemberRepository.saveAllAndFlush(captor.capture())).thenAnswer(i -> i.getArgument(0));

        chatRoomService.createGroupChat(currentAccount, req);

        List<ChatMember> saved = captor.getValue();
        // owner should be present plus two members = 3
        assertThat(saved).hasSize(3);
        assertThat(saved).anyMatch(m -> m.getAccount().getAccountId() == currentAccount.getAccountId() && m.getRole() == ChatRole.OWNER);
    }

    @Test
    void createGroup_largeMemberList_savesAllMembers() throws Exception {
        List<Long> ids = List.of(2000L,2001L,2002L,2003L,2004L);
        CreateGroupChatRequest req = new CreateGroupChatRequest(ids, "Big", null);
        Account[] accs = new Account[ids.size()];
        for (int i=0;i<ids.size();i++) { Account u = new User(); u.setAccountId(ids.get(i)); accs[i]=u; }

        when(accountRepository.findAllByAccountIdInAndDeletedAtIsNull(req.getAccountIds())).thenReturn(List.of(accs));
        when(chatRoomRepository.saveAndFlush(any())).thenAnswer(invocation -> { ChatRoom r = invocation.getArgument(0); r.setRoomId(999L); return r; });
        when(chatMemberRepository.saveAllAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        ChatRoom created = chatRoomService.createGroupChat(currentAccount, req);
        assertThat(created.getRoomId()).isEqualTo(999L);
    }
}
