package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.MessageEvent;
import iuh.fit.goat.dto.request.poll.AddPollOptionRequest;
import iuh.fit.goat.dto.request.poll.ClosePollRequest;
import iuh.fit.goat.dto.request.poll.CreatePollRequest;
import iuh.fit.goat.dto.request.poll.VotePollRequest;
import iuh.fit.goat.dto.response.poll.*;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.entity.embeddable.SenderInfo;
import iuh.fit.goat.enumeration.MessageType;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.repository.PollRepository;
import iuh.fit.goat.repository.PollVoteRepository;
import iuh.fit.goat.service.PollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PollServiceImpl implements PollService {

    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public PollResponse createPoll(Long chatRoomId, CreatePollRequest request, Account currentAccount) throws InvalidException
    {
        validChatRoomAndMembership(chatRoomId, currentAccount.getAccountId());

        String pollId = generatePollId();
        String messageId = generateMessageId();

        List<PollOption> options = request.getOptions().stream()
                .map(optionText -> PollOption.builder()
                        .optionId(generateOptionId())
                        .text(optionText)
                        .createdBy(currentAccount.getEmail())
                        .createdAt(Instant.now())
                        .voteCount(0)
                        .build()
                )
                .collect(Collectors.toList());

        Poll poll = Poll.builder()
                .pollId(pollId)
                .chatRoomId(chatRoomId)
                .messageId(messageId)
                .createdBy(currentAccount.getEmail())
                .question(request.getQuestion())
                .options(options)
                .multipleChoice(request.getMultipleChoice())
                .allowAddOption(request.getAllowAddOption())
                .pinned(request.getPinned())
                .isClosed(false)
                .expiresAt(request.getExpiresAt())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        this.pollRepository.save(poll);

        Message message = Message.builder()
                .messageId(messageId)
                .messageSk(Message.buildMessageSk(System.currentTimeMillis(), messageId))
                .chatRoomId(chatRoomId.toString())
                .sender(getSenderInfo(currentAccount))
                .content(request.getQuestion())
                .messageType(MessageType.POLL)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isHidden(false)
                .isForwarded(false)
                .build();
        this.messageRepository.saveMessage(message);

        if (poll.getPinned() != null && poll.getPinned()) {
            this.messageRepository.pinMessage(
                    chatRoomId.toString(), messageId,
                    currentAccount instanceof Company company ? company.getName() : ((User) Objects.requireNonNull(currentAccount)).getFullName()
            );
        }

        PollResponse pollResponse = toPollResponse(poll, currentAccount.getAccountId());
        sendPollCreatedEvent(chatRoomId, pollResponse);

        return pollResponse;
    }

    @Override
    public PollResponse votePoll(Long chatRoomId, VotePollRequest request, Account currentAccount) throws InvalidException
    {
        validChatRoomAndMembership(chatRoomId, currentAccount.getAccountId());

        Poll poll = this.pollRepository.findByPollId(request.getPollId())
                .orElseThrow(() -> new InvalidException("Bình chọn không tồn tại"));

        if (poll.getIsClosed()) throw new InvalidException("Bình chọn đã đóng");

        if (poll.getExpiresAt() != null && Instant.now().isAfter(poll.getExpiresAt())) {
            poll.setIsClosed(true);
            this.pollRepository.save(poll);
            throw new InvalidException("Bình chọn đã hết hạn");
        }

        Long accountId = currentAccount.getAccountId();
        List<PollVote> existingVotes = this.pollVoteRepository.findByPollIdAndAccountId(poll.getPollId(), accountId);

        if (!poll.getMultipleChoice() && !existingVotes.isEmpty()) {
            for (PollVote existingVote : existingVotes) {
                PollOption option = poll.getOptions().stream()
                        .filter(o -> o.getOptionId().equals(existingVote.getOptionId()))
                        .findFirst()
                        .orElse(null);
                if (option != null && option.getVoteCount() > 0) {
                    option.setVoteCount(option.getVoteCount() - 1);
                }
                this.pollVoteRepository.delete(existingVote.getVoteId());
            }
        }

        for (String optionId : request.getOptionIds()) {
            PollOption option = poll.getOptions().stream()
                    .filter(o -> o.getOptionId().equals(optionId))
                    .findFirst()
                    .orElseThrow(() -> new InvalidException("Không tìm thấy lựa chọn: " + optionId));

            PollVote vote = PollVote.builder()
                    .voteId(generateVoteId())
                    .pollId(poll.getPollId())
                    .optionId(optionId)
                    .accountId(accountId)
                    .createdAt(Instant.now())
                    .build();

            this.pollVoteRepository.save(vote);
            option.setVoteCount(option.getVoteCount() + 1);
        }

        poll.setUpdatedAt(Instant.now());
        this.pollRepository.save(poll);

        PollResponse pollResponse = toPollResponse(poll, currentAccount.getAccountId());
        int totalVotes = poll.getOptions().stream()
                .mapToInt(o -> o.getVoteCount() != null ? o.getVoteCount() : 0)
                .sum();

        sendPollVotedEvent(chatRoomId, request.getPollId(), accountId.toString(), request.getOptionIds(), totalVotes);

        return pollResponse;
    }

    @Override
    public PollResponse addOptionToPoll(Long chatRoomId, AddPollOptionRequest request, Account currentAccount) throws InvalidException
    {
        validChatRoomAndMembership(chatRoomId, currentAccount.getAccountId());

        Poll poll = this.pollRepository.findByPollId(request.getPollId())
                .orElseThrow(() -> new InvalidException("Bình chọn không tồn tại"));

        if (!poll.getAllowAddOption()) throw new InvalidException("Không được phép thêm lựa chọn vào bình chọn này");
        if (poll.getIsClosed()) throw new InvalidException("Bình chọn đã đóng");
        if (poll.getOptions().size() >= 10) throw new InvalidException("Chỉ được phép có tối đa 10 lựa chọn trong một bình chọn");

        PollOption newOption = PollOption.builder()
                .optionId(generateOptionId())
                .text(request.getText())
                .createdBy(currentAccount.getEmail())
                .createdAt(Instant.now())
                .voteCount(0)
                .build();

        poll.getOptions().add(newOption);
        poll.setUpdatedAt(Instant.now());
        this.pollRepository.save(poll);

        PollResponse pollResponse = toPollResponse(poll, currentAccount.getAccountId());
        sendPollOptionAddedEvent(chatRoomId, poll.getPollId(), newOption);

        return pollResponse;
    }

    @Override
    public PollResponse closePoll(Long chatRoomId, ClosePollRequest request, Account currentAccount) throws InvalidException
    {
        validChatRoomAndMembership(chatRoomId, currentAccount.getAccountId());

        Poll poll = this.pollRepository.findByPollId(request.getPollId())
                .orElseThrow(() -> new InvalidException("Bình chọn không tồn tại"));

        if (!poll.getCreatedBy().equals(currentAccount.getEmail())) {
            throw new InvalidException("Chỉ người tạo bình chọn mới có quyền đóng bình chọn");
        }

        poll.setIsClosed(true);
        poll.setUpdatedAt(Instant.now());
        this.pollRepository.save(poll);

        sendPollClosedEvent(chatRoomId, poll.getPollId(), poll.getMessageId());

        return toPollResponse(poll, currentAccount.getAccountId());
    }

    @Override
    @Transactional(readOnly = true)
    public PollResponse getPoll(String pollId, Account currentAccount) throws InvalidException{
        Poll poll = pollRepository.findByPollId(pollId)
                .orElseThrow(() -> new InvalidException("Bình chọn không tồn tại"));

        return toPollResponse(poll, currentAccount.getAccountId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAddOptionToPoll(String pollId) throws InvalidException {
        Poll poll = this.pollRepository.findByPollId(pollId)
                .orElseThrow(() -> new InvalidException("Bình chọn không tồn tại"));

        return poll.getAllowAddOption() && !poll.getIsClosed() &&
                (poll.getExpiresAt() == null || Instant.now().isBefore(poll.getExpiresAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccountVoted(String pollId, Long accountId) throws InvalidException {
        Poll poll = pollRepository.findByPollId(pollId)
                .orElseThrow(() -> new InvalidException("Bình chọn không tồn tại"));

        return !pollVoteRepository.findByPollIdAndAccountId(poll.getPollId(), accountId).isEmpty();
    }

    private SenderInfo getSenderInfo(Account account) {
        String fullName = account instanceof Company company ? company.getName() : ((User) account).getFullName();
        String avatar = account instanceof Company company ? company.getLogo() : account.getAvatar();
        return SenderInfo.builder()
                .accountId(account.getAccountId())
                .fullName(fullName)
                .username(account.getUsername())
                .email(account.getEmail())
                .avatar(avatar)
                .build();
    }

    private PollResponse toPollResponse(Poll poll, Long currentAccountId) {
        List<PollOptionResponse> optionResponses = poll.getOptions().stream()
                .map(option -> {
                    List<PollVote> accountVotes = this.pollVoteRepository.findByPollIdAndAccountId(poll.getPollId(), currentAccountId);
                    boolean accountVoted = accountVotes.stream()
                            .anyMatch(v -> v.getOptionId().equals(option.getOptionId()));

                    return PollOptionResponse.builder()
                            .optionId(option.getOptionId())
                            .text(option.getText())
                            .createdBy(option.getCreatedBy() != null ? option.getCreatedBy() : null)
                            .createdAt(option.getCreatedAt())
                            .voteCount(option.getVoteCount() != null ? option.getVoteCount() : 0)
                            .accountVoted(accountVoted)
                            .build();
                })
                .collect(Collectors.toList());

        return PollResponse.builder()
                .pollId(poll.getPollId())
                .chatRoomId(poll.getChatRoomId())
                .messageId(poll.getMessageId())
                .createdBy(poll.getCreatedBy() != null ? poll.getCreatedBy() : null)
                .question(poll.getQuestion())
                .options(optionResponses)
                .multipleChoice(poll.getMultipleChoice())
                .allowAddOption(poll.getAllowAddOption())
                .pinned(poll.getPinned())
                .isClosed(poll.getIsClosed())
                .expiresAt(poll.getExpiresAt())
                .createdAt(poll.getCreatedAt())
                .updatedAt(poll.getUpdatedAt())
                .build();
    }

    private void sendPollCreatedEvent(Long chatRoomId, PollResponse pollResponse) {
        PollCreatedEventResponse event = PollCreatedEventResponse.builder()
                .eventType(MessageEvent.POLL_CREATED)
                .pollId(pollResponse.getPollId())
                .chatRoomId(chatRoomId.toString())
                .messageId(pollResponse.getMessageId())
                .poll(pollResponse)
                .createdAt(Instant.now())
                .build();

        this.messagingTemplate.convertAndSend("/topic/chatrooms/" + chatRoomId, event);
        log.debug("Sent POLL_CREATED event to chatRoom: {}", chatRoomId);
    }

    private void sendPollVotedEvent(
            Long chatRoomId, String pollId, String accountId, List<String> optionIds, Integer totalVotes
    )
    {
        PollVotedEventResponse event = PollVotedEventResponse.builder()
                .eventType(MessageEvent.POLL_VOTED)
                .pollId(pollId)
                .chatRoomId(chatRoomId.toString())
                .accountId(accountId)
                .optionIds(optionIds)
                .totalVotes(totalVotes)
                .votedAt(Instant.now())
                .build();

        this.messagingTemplate.convertAndSend("/topic/chatrooms/" + chatRoomId, event);
        log.debug("Sent POLL_VOTED event to chatRoom: {}", chatRoomId);
    }

    private void sendPollOptionAddedEvent(Long chatRoomId, String pollId, PollOption option) {
        PollOptionAddedEventResponse event = PollOptionAddedEventResponse.builder()
                .eventType(MessageEvent.POLL_OPTION_ADDED)
                .pollId(pollId)
                .chatRoomId(chatRoomId.toString())
                .optionId(option.getOptionId())
                .text(option.getText())
                .createdBy(option.getCreatedBy())
                .createdAt(option.getCreatedAt())
                .build();

        this.messagingTemplate.convertAndSend("/topic/chatrooms/" + chatRoomId, event);
        log.debug("Sent POLL_OPTION_ADDED event to chatRoom: {}", chatRoomId);
    }

    private void sendPollClosedEvent(Long chatRoomId, String pollId, String messageId) {
        PollClosedEventResponse event = PollClosedEventResponse.builder()
                .eventType(MessageEvent.POLL_CLOSED)
                .pollId(pollId)
                .chatRoomId(chatRoomId.toString())
                .messageId(messageId)
                .closedAt(Instant.now())
                .build();

        this.messagingTemplate.convertAndSend("/topic/chatrooms/" + chatRoomId, event);
        log.debug("Sent POLL_CLOSED event to chatRoom: {}", chatRoomId);
    }

    private void validChatRoomAndMembership(Long chatRoomId, Long currentAccountId) throws InvalidException {
        ChatRoom chatRoom = this.chatRoomRepository.findByRoomIdAndDeletedAtIsNull(chatRoomId)
                .orElseThrow(() -> new InvalidException("Phòng chat không tồn tại"));

        boolean isMember = chatRoom.getMembers().stream()
                .anyMatch(
                        m -> m.getDeletedAt() == null && m.getAccount().getAccountId() == currentAccountId
                );
        if (!isMember) throw new InvalidException("Nguời dùng không phải là thành viên của phòng chat này");
    }

    private String generatePollId() {
        return "poll_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateOptionId() {
        return "opt_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateVoteId() {
        return "vote_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateMessageId() {
        return "msg_" + UUID.randomUUID().toString().replace("-", "");
    }
}

