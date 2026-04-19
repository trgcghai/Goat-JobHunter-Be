package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.poll.AddPollOptionRequest;
import iuh.fit.goat.dto.request.poll.ClosePollRequest;
import iuh.fit.goat.dto.request.poll.CreatePollRequest;
import iuh.fit.goat.dto.request.poll.VotePollRequest;
import iuh.fit.goat.dto.response.poll.PollResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.AccountService;
import iuh.fit.goat.service.PollService;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/chatrooms/{chatRoomId}/polls")
@RequiredArgsConstructor
public class PollController {
    private final PollService pollService;
    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<PollResponse> createPoll(
            @PathVariable Long chatRoomId, @Valid @RequestBody CreatePollRequest request
    ) throws InvalidException
    {
        String email = SecurityUtil.getCurrentUserEmail();
        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if (currentAccount == null) throw new InvalidException("Tài khoản không tồn tại");

        PollResponse response = this.pollService.createPoll(chatRoomId, request, currentAccount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/vote")
    public ResponseEntity<PollResponse> votePoll(
            @PathVariable Long chatRoomId, @Valid @RequestBody VotePollRequest request
    ) throws InvalidException
    {
        String email = SecurityUtil.getCurrentUserEmail();
        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if (currentAccount == null) throw new InvalidException("Tài khoản không tồn tại");

        PollResponse response = this.pollService.votePoll(chatRoomId, request, currentAccount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add-option")
    public ResponseEntity<PollResponse> addOptionToPoll(
            @PathVariable Long chatRoomId, @Valid @RequestBody AddPollOptionRequest request
    ) throws InvalidException
    {
        String email = SecurityUtil.getCurrentUserEmail();
        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if (currentAccount == null) throw new InvalidException("Tài khoản không tồn tại");

        PollResponse response = this.pollService.addOptionToPoll(chatRoomId, request, currentAccount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/close")
    public ResponseEntity<PollResponse> closePoll(
            @PathVariable Long chatRoomId, @Valid @RequestBody ClosePollRequest request
    ) throws InvalidException
    {
        String email = SecurityUtil.getCurrentUserEmail();
        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if (currentAccount == null) throw new InvalidException("Tài khoản không tồn tại");

        PollResponse response = this.pollService.closePoll(chatRoomId, request, currentAccount);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{pollId}")
    public ResponseEntity<PollResponse> getPoll(@PathVariable String pollId) throws InvalidException {
        String email = SecurityUtil.getCurrentUserEmail();
        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if (currentAccount == null) throw new InvalidException("Tài khoản không tồn tại");

        PollResponse response = this.pollService.getPoll(pollId, currentAccount);
        return ResponseEntity.ok(response);
    }
}

