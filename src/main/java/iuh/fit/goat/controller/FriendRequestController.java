package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.friendship.CreateFriendRequestRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.friendship.FriendRequestResponse;
import iuh.fit.goat.exception.ConflictException;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.NotFoundException;
import iuh.fit.goat.service.FriendRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/friend-requests")
@RequiredArgsConstructor
public class FriendRequestController {
    private final FriendRequestService friendRequestService;

    @GetMapping("/me")
    public ResponseEntity<ResultPaginationResponse> getMyFriends(Pageable pageable) throws InvalidException {
        ResultPaginationResponse response = this.friendRequestService.handleGetMyFriends(pageable);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/me/received")
    public ResponseEntity<ResultPaginationResponse> getMyReceivedFriendRequests(Pageable pageable) throws InvalidException {
        ResultPaginationResponse response = this.friendRequestService.handleGetMyReceivedFriendRequests(pageable);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/me/sent")
    public ResponseEntity<ResultPaginationResponse> getMySentFriendRequests(Pageable pageable) throws InvalidException {
        ResultPaginationResponse response = this.friendRequestService.handleGetMySentFriendRequests(pageable);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping
    public ResponseEntity<FriendRequestResponse> createFriendRequest(@Valid @RequestBody CreateFriendRequestRequest request)
            throws InvalidException, ConflictException, NotFoundException {
        FriendRequestResponse response = this.friendRequestService.handleCreateFriendRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<FriendRequestResponse> acceptFriendRequest(@PathVariable("id") Long id)
            throws InvalidException, ConflictException, NotFoundException {
        FriendRequestResponse response = this.friendRequestService.handleAcceptFriendRequest(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<FriendRequestResponse> rejectFriendRequest(@PathVariable("id") Long id)
            throws InvalidException, ConflictException, NotFoundException {
        FriendRequestResponse response = this.friendRequestService.handleRejectFriendRequest(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<FriendRequestResponse> cancelFriendRequest(@PathVariable("id") Long id)
            throws InvalidException, ConflictException, NotFoundException {
        FriendRequestResponse response = this.friendRequestService.handleCancelFriendRequest(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<FriendRequestResponse> blockUser(@PathVariable("id") Long id)
            throws InvalidException, NotFoundException {
        FriendRequestResponse response = this.friendRequestService.handleBlockUser(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{id}/unblock")
    public ResponseEntity<FriendRequestResponse> unblockUser(@PathVariable("id") Long id)
            throws InvalidException, NotFoundException {
        FriendRequestResponse response = this.friendRequestService.handleUnblockUser(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
