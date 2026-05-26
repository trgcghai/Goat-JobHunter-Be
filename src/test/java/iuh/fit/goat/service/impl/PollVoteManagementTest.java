package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.MessageEvent;
import iuh.fit.goat.dto.request.poll.VotePollRequest;
import iuh.fit.goat.dto.response.poll.PollResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.ChatRole;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.*;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PollVoteManagementTest {

    @Mock private PollRepository pollRepository;
    @Mock private PollVoteRepository pollVoteRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private MessageService messageService;
    @Mock private ChatRoomPermissionGuard chatRoomPermissionGuard;

    @InjectMocks private PollServiceImpl pollService;

    private User currentUser;
    private ChatRoom chatRoom;
    private ChatMember member;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setAccountId(500L);
        currentUser.setEmail("voter@example.com");
        currentUser.setUsername("voter");

        chatRoom = new ChatRoom();
        chatRoom.setRoomId(200L);
        member = new ChatMember();
        member.setAccount(currentUser);
        member.setRole(ChatRole.MEMBER);
        chatRoom.setMembers(new java.util.ArrayList<>(List.of(member)));

        // common lenient stubs
        lenient().when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(200L)).thenReturn(Optional.of(chatRoom));
    }

    @Test
    void vote_singleChoice_success_incrementsCount_and_sendsEvent() throws Exception {
        PollOption opt = PollOption.builder().optionId("opt1").text("A").voteCount(0).createdAt(Instant.now()).build();
        Poll poll = Poll.builder().pollId("poll1").chatRoomId(200L).options(new java.util.ArrayList<>(List.of(opt))).isClosed(false).multipleChoice(false).createdAt(Instant.now()).build();

        when(pollRepository.findByPollId(200L, "poll1")).thenReturn(Optional.of(poll));
        when(pollVoteRepository.findByPollIdAndAccountId("poll1", 500L)).thenReturn(List.of());
        when(pollVoteRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PollResponse res = pollService.votePoll(200L, new VotePollRequest("poll1", List.of("opt1")), currentUser);

        assertThat(poll.getOptions().get(0).getVoteCount()).isEqualTo(1);
        verify(messageService).createAndSendPollMessage(eq(200L), eq(MessageEvent.POLL_VOTED), eq(currentUser), any(PollResponse.class));
    }

    @Test
    void vote_unvote_with_emptyOptionIds_removesExistingVotes_and_sendsUnvote() throws Exception {
        PollOption opt = PollOption.builder().optionId("opt1").text("A").voteCount(2).createdAt(Instant.now()).build();
        Poll poll = Poll.builder().pollId("poll2").chatRoomId(200L).options(new java.util.ArrayList<>(List.of(opt))).isClosed(false).multipleChoice(false).createdAt(Instant.now()).build();

        PollVote existing = PollVote.builder().voteId("v1").pollId("poll2").optionId("opt1").accountId(500L).build();

        when(pollRepository.findByPollId(200L, "poll2")).thenReturn(Optional.of(poll));
        when(pollVoteRepository.findByPollIdAndAccountId("poll2", 500L)).thenReturn(List.of(existing));
        doNothing().when(pollVoteRepository).delete("v1");

        PollResponse res = pollService.votePoll(200L, new VotePollRequest("poll2", List.of()), currentUser);

        assertThat(poll.getOptions().get(0).getVoteCount()).isEqualTo(1);
        verify(messageService).createAndSendPollMessage(eq(200L), eq(MessageEvent.POLL_UNVOTED), eq(currentUser), any(PollResponse.class));
    }

    @Test
    void vote_changeVote_decrementsPrevious_and_incrementsNew() throws Exception {
        PollOption o1 = PollOption.builder().optionId("o1").text("One").voteCount(3).createdAt(Instant.now()).build();
        PollOption o2 = PollOption.builder().optionId("o2").text("Two").voteCount(0).createdAt(Instant.now()).build();
        Poll poll = Poll.builder().pollId("poll3").chatRoomId(200L).options(new java.util.ArrayList<>(List.of(o1,o2))).isClosed(false).multipleChoice(false).createdAt(Instant.now()).build();

        PollVote existing = PollVote.builder().voteId("vv1").pollId("poll3").optionId("o1").accountId(500L).build();

        when(pollRepository.findByPollId(200L, "poll3")).thenReturn(Optional.of(poll));
        when(pollVoteRepository.findByPollIdAndAccountId("poll3", 500L)).thenReturn(List.of(existing));
        doNothing().when(pollVoteRepository).delete("vv1");
        when(pollVoteRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        pollService.votePoll(200L, new VotePollRequest("poll3", List.of("o2")), currentUser);

        assertThat(o1.getVoteCount()).isEqualTo(2);
        assertThat(o2.getVoteCount()).isEqualTo(1);
        verify(pollVoteRepository).delete("vv1");
        verify(pollVoteRepository).save(any());
    }

    @Test
    void vote_multipleChoice_allowsMultipleOptionIds() throws Exception {
        PollOption a = PollOption.builder().optionId("a").text("A").voteCount(0).createdAt(Instant.now()).build();
        PollOption b = PollOption.builder().optionId("b").text("B").voteCount(0).createdAt(Instant.now()).build();
        Poll poll = Poll.builder().pollId("poll4").chatRoomId(200L).options(new java.util.ArrayList<>(List.of(a,b))).isClosed(false).multipleChoice(true).createdAt(Instant.now()).build();

        when(pollRepository.findByPollId(200L, "poll4")).thenReturn(Optional.of(poll));
        when(pollVoteRepository.findByPollIdAndAccountId("poll4", 500L)).thenReturn(List.of());
        when(pollVoteRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        pollService.votePoll(200L, new VotePollRequest("poll4", List.of("a","b")), currentUser);

        assertThat(a.getVoteCount()).isEqualTo(1);
        assertThat(b.getVoteCount()).isEqualTo(1);
    }

    @Test
    void vote_invalidOptionId_throwsInvalid() throws Exception {
        PollOption opt = PollOption.builder().optionId("x").text("X").voteCount(0).createdAt(Instant.now()).build();
        Poll poll = Poll.builder().pollId("poll5").chatRoomId(200L).options(new java.util.ArrayList<>(List.of(opt))).isClosed(false).multipleChoice(false).createdAt(Instant.now()).build();

        when(pollRepository.findByPollId(200L, "poll5")).thenReturn(Optional.of(poll));
        when(pollVoteRepository.findByPollIdAndAccountId("poll5", 500L)).thenReturn(List.of());

        assertThrows(InvalidException.class, () -> pollService.votePoll(200L, new VotePollRequest("poll5", List.of("nonexistent")), currentUser));
    }

    @Test
    void vote_pollClosed_throwsInvalid() throws Exception {
        Poll poll = Poll.builder().pollId("poll6").chatRoomId(200L).options(new java.util.ArrayList<>()).isClosed(true).createdAt(Instant.now()).build();
        when(pollRepository.findByPollId(200L, "poll6")).thenReturn(Optional.of(poll));

        assertThrows(InvalidException.class, () -> pollService.votePoll(200L, new VotePollRequest("poll6", List.of("any")), currentUser));
    }

    @Test
    void vote_pollExpired_setsClosedAnd_throwsInvalid() throws Exception {
        Poll poll = Poll.builder().pollId("poll7").chatRoomId(200L).options(new java.util.ArrayList<>()).isClosed(false).expiresAt(Instant.now().minusSeconds(60)).createdAt(Instant.now()).build();
        when(pollRepository.findByPollId(200L, "poll7")).thenReturn(Optional.of(poll));
        when(pollRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThrows(InvalidException.class, () -> pollService.votePoll(200L, new VotePollRequest("poll7", List.of("any")), currentUser));
        assertThat(poll.getIsClosed()).isTrue();
    }

    @Test
    void vote_nonMember_throwsInvalid() {
        ChatRoom otherRoom = new ChatRoom(); otherRoom.setRoomId(201L); otherRoom.setMembers(new java.util.ArrayList<>());
        when(chatRoomRepository.findByRoomIdAndDeletedAtIsNull(201L)).thenReturn(Optional.of(otherRoom));

        assertThrows(InvalidException.class, () -> pollService.votePoll(201L, new VotePollRequest("p", List.of("o")), currentUser));
    }

    @Test
    void vote_unvote_whenExistingVotes_deletesAnd_sendsUnvoteEvent() throws Exception {
        PollOption opt = PollOption.builder().optionId("optU").text("U").voteCount(1).createdAt(Instant.now()).build();
        Poll poll = Poll.builder().pollId("pollU").chatRoomId(200L).options(new java.util.ArrayList<>(List.of(opt))).isClosed(false).createdAt(Instant.now()).build();
        PollVote v = PollVote.builder().voteId("vv").pollId("pollU").optionId("optU").accountId(500L).build();

        when(pollRepository.findByPollId(200L, "pollU")).thenReturn(Optional.of(poll));
        when(pollVoteRepository.findByPollIdAndAccountId("pollU", 500L)).thenReturn(List.of(v));

        pollService.votePoll(200L, new VotePollRequest("pollU", List.of()), currentUser);

        verify(pollVoteRepository).delete("vv");
        verify(messageService).createAndSendPollMessage(eq(200L), eq(MessageEvent.POLL_UNVOTED), eq(currentUser), any(PollResponse.class));
    }
}
