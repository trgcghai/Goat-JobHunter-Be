package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.poll.CreatePollRequest;
import iuh.fit.goat.dto.request.poll.VotePollRequest;
import iuh.fit.goat.dto.response.poll.PollResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.ChatRole;
import iuh.fit.goat.enumeration.ChatRoomPermissionAction;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.PollRepository;
import iuh.fit.goat.repository.PollVoteRepository;
import iuh.fit.goat.service.MessageService;
import iuh.fit.goat.service.helper.ChatRoomPermissionGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollServiceImplTest {

    @Mock private PollRepository pollRepository;
    @Mock private PollVoteRepository pollVoteRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private MessageService messageService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ChatRoomPermissionGuard chatRoomPermissionGuard;

    @InjectMocks
    private PollServiceImpl pollService;

    private User currentUser;
    private ChatRoom chatRoom;
    private ChatMember member;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setAccountId(9001L);
        currentUser.setEmail("poller@example.com");
        currentUser.setUsername("poller");

        chatRoom = new ChatRoom();
        chatRoom.setRoomId(123L);
        chatRoom.setMembers(new java.util.ArrayList<>());

        member = new ChatMember();
        member.setAccount(currentUser);
        member.setRole(ChatRole.OWNER);
        chatRoom.getMembers().add(member);
    }

    @Test
    void createPoll_shouldSavePollAndSendMessage() throws Exception {
        CreatePollRequest request = new CreatePollRequest(
                "Best stack?",
                List.of("Java", "Kotlin"),
                false,
                true,
                false,
                null
        );

        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(123L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 9001L)).thenReturn(member);
        doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.CREATE_POLL);
        when(pollRepository.save(any(Poll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PollResponse response = pollService.createPoll(123L, request, currentUser);

        assertThat(response.getQuestion()).isEqualTo("Best stack?");
        assertThat(response.getOptions()).hasSize(2);
        verify(messageService).createAndSendPollMessage(eq(123L), any(), eq(currentUser), any(PollResponse.class));
    }

    @Test
    void votePoll_shouldCreateVotesAndUpdateCounters() throws Exception {
        PollOption option = PollOption.builder().optionId("opt_1").text("Java").createdBy("poller@example.com").createdAt(Instant.now()).voteCount(0).build();
        Poll poll = Poll.builder()
                .pollId("poll_1")
                .chatRoomId(123L)
                .messageId("msg_1")
                .createdBy("poller@example.com")
                .question("Best stack?")
                .options(new java.util.ArrayList<>(List.of(option)))
                .multipleChoice(false)
                .allowAddOption(true)
                .pinned(false)
                .isClosed(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(123L)).thenReturn(Optional.of(chatRoom));
        when(pollRepository.findByPollId(123L, "poll_1")).thenReturn(Optional.of(poll));
        when(pollVoteRepository.findByPollIdAndAccountId("poll_1", 9001L)).thenReturn(List.of());
        when(pollVoteRepository.save(any(PollVote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PollResponse response = pollService.votePoll(123L, new VotePollRequest("poll_1", List.of("opt_1")), currentUser);

        assertThat(response.getPollId()).isEqualTo("poll_1");
        assertThat(poll.getOptions().getFirst().getVoteCount()).isEqualTo(1);
        verify(messageService).createAndSendPollMessage(eq(123L), any(), eq(currentUser), any(PollResponse.class));
    }
}