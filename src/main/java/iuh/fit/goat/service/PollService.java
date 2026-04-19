package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.poll.AddPollOptionRequest;
import iuh.fit.goat.dto.request.poll.ClosePollRequest;
import iuh.fit.goat.dto.request.poll.CreatePollRequest;
import iuh.fit.goat.dto.request.poll.VotePollRequest;
import iuh.fit.goat.dto.response.poll.PollResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.exception.InvalidException;

public interface PollService {
    PollResponse createPoll(Long chatRoomId, CreatePollRequest request, Account currentAccount) throws InvalidException;

    PollResponse votePoll(Long chatRoomId, VotePollRequest request, Account currentAccount) throws InvalidException;

    PollResponse addOptionToPoll(Long chatRoomId, AddPollOptionRequest request, Account currentAccount) throws InvalidException;

    PollResponse closePoll(Long chatRoomId, ClosePollRequest request, Account currentAccount) throws InvalidException;

    PollResponse getPoll(String pollId, Account currentAccount) throws InvalidException;

    boolean canAddOptionToPoll(String pollId) throws InvalidException;

    boolean hasAccountVoted(String pollId, Long accountId) throws InvalidException;
}

