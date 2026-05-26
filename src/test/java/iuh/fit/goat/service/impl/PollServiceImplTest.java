package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.poll.CreatePollRequest;
import iuh.fit.goat.dto.request.poll.VotePollRequest;
import iuh.fit.goat.dto.response.poll.PollResponse;
import iuh.fit.goat.common.MessageEvent;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.ChatRole;
import iuh.fit.goat.enumeration.ChatRoomPermissionAction;
import iuh.fit.goat.exception.InvalidException;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    void setUp() throws Exception {
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

        lenient().when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(123L)).thenReturn(Optional.of(chatRoom));
        lenient().when(chatRoomPermissionGuard.getCurrentMember(chatRoom, 9001L)).thenReturn(member);
        lenient().doNothing().when(chatRoomPermissionGuard).assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.CREATE_POLL);
        lenient().when(pollVoteRepository.findByPollIdAndAccountId(any(), eq(9001L))).thenReturn(List.of());
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

        when(pollRepository.save(any(Poll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PollResponse response = pollService.createPoll(123L, request, currentUser);

        assertThat(response.getQuestion()).isEqualTo("Best stack?");
        assertThat(response.getOptions()).hasSize(2);
        verify(messageService).createAndSendPollMessage(eq(123L), any(), eq(currentUser), any(PollResponse.class));
    }

        @Test
        void createPoll_shouldSetMultipleChoiceAndPinnedFlags() throws Exception {
        CreatePollRequest request = new CreatePollRequest(
            "Feature choice?",
            List.of("A", "B"),
            true,
            true,
            true,
            null
        );

        when(pollRepository.save(any(Poll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PollResponse response = pollService.createPoll(123L, request, currentUser);

        assertThat(response.getMultipleChoice()).isTrue();
        assertThat(response.getPinned()).isTrue();
        assertThat(response.getAllowAddOption()).isTrue();
        }

        @Test
        void createPoll_shouldPropagateExpiresAt() throws Exception {
        Instant expiresAt = Instant.parse("2026-12-31T23:59:59Z");
        CreatePollRequest request = new CreatePollRequest(
            "Expires?",
            List.of("Yes", "No"),
            false,
            false,
            false,
            expiresAt
        );

        when(pollRepository.save(any(Poll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PollResponse response = pollService.createPoll(123L, request, currentUser);

        assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
        }

        @Test
        void createPoll_shouldGeneratePollAndMessageIds() throws Exception {
        CreatePollRequest request = new CreatePollRequest(
            "IDs?",
            List.of("One", "Two"),
            false,
            false,
            false,
            null
        );

        when(pollRepository.save(any(Poll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PollResponse response = pollService.createPoll(123L, request, currentUser);

        assertThat(response.getPollId()).startsWith("poll_");
        assertThat(response.getMessageId()).startsWith("msg_");
        assertThat(response.getOptions()).allSatisfy(option -> assertThat(option.getOptionId()).startsWith("opt_"));
        }

        @Test
        void createPoll_shouldPreserveOptionOrder() throws Exception {
        CreatePollRequest request = new CreatePollRequest(
            "Order?",
            List.of("First", "Second", "Third"),
            false,
            false,
            false,
            null
        );

        when(pollRepository.save(any(Poll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PollResponse response = pollService.createPoll(123L, request, currentUser);

        assertThat(response.getOptions()).extracting("text").containsExactly("First", "Second", "Third");
        }

        @Test
        void createPoll_shouldInitializeVoteCountsToZero() throws Exception {
        CreatePollRequest request = new CreatePollRequest(
            "Zero counts?",
            List.of("One", "Two"),
            false,
            false,
            false,
            null
        );

        when(pollRepository.save(any(Poll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PollResponse response = pollService.createPoll(123L, request, currentUser);

        assertThat(response.getOptions()).extracting("voteCount").containsExactly(0, 0);
        }

        @Test
        void createPoll_shouldSendPollCreatedEvent() throws Exception {
        CreatePollRequest request = new CreatePollRequest(
            "Event?",
            List.of("Yes", "No"),
            false,
            false,
            false,
            null
        );

        when(pollRepository.save(any(Poll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pollService.createPoll(123L, request, currentUser);

        verify(messageService).createAndSendPollMessage(eq(123L), eq(MessageEvent.POLL_CREATED), eq(currentUser), any(PollResponse.class));
        }

        @Test
        void createPoll_roomNotFound_throwsInvalid() {
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(123L)).thenReturn(Optional.empty());

        CreatePollRequest request = new CreatePollRequest(
            "Missing room?",
            List.of("A", "B"),
            false,
            false,
            false,
            null
        );

        assertThatThrownBy(() -> pollService.createPoll(123L, request, currentUser))
            .isInstanceOf(InvalidException.class)
            .hasMessageContaining("Phòng chat không tồn tại");
        }

        @Test
        void createPoll_nonMember_throwsInvalid() {
        Account outsider = new User();
        outsider.setAccountId(9999L);
        outsider.setEmail("outsider@example.com");
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(123L)).thenReturn(Optional.of(chatRoom));

        CreatePollRequest request = new CreatePollRequest(
            "Non-member?",
            List.of("A", "B"),
            false,
            false,
            false,
            null
        );

        assertThatThrownBy(() -> pollService.createPoll(123L, request, outsider))
            .isInstanceOf(InvalidException.class)
            .hasMessageContaining("Nguời dùng không phải là thành viên");
        }

        @Test
        void createPoll_permissionDenied_throwsInvalid() throws Exception {
        doThrow(new InvalidException("denied"))
            .when(chatRoomPermissionGuard)
            .assertCanPerformAction(chatRoom, member, ChatRoomPermissionAction.CREATE_POLL);

        CreatePollRequest request = new CreatePollRequest(
            "No permission?",
            List.of("A", "B"),
            false,
            false,
            false,
            null
        );

        assertThatThrownBy(() -> pollService.createPoll(123L, request, currentUser))
            .isInstanceOf(InvalidException.class)
            .hasMessageContaining("denied");
        }

        @Test
        void createPoll_shouldPopulateAuthorMetadataInOptions() throws Exception {
        CreatePollRequest request = new CreatePollRequest(
            "Metadata?",
            List.of("A", "B"),
            false,
            true,
            false,
            null
        );

        when(pollRepository.save(any(Poll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PollResponse response = pollService.createPoll(123L, request, currentUser);

        assertThat(response.getOptions()).allSatisfy(option -> assertThat(option.getCreatedBy()).isEqualTo("poller@example.com"));
        }

        @Test
        void createPoll_withTenOptions_succeeds() throws Exception {
        CreatePollRequest request = new CreatePollRequest(
            "Max options?",
            List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
            false,
            false,
            false,
            null
        );

        when(pollRepository.save(any(Poll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PollResponse response = pollService.createPoll(123L, request, currentUser);

        assertThat(response.getOptions()).hasSize(10);
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