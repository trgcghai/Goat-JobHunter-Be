package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.friendship.CreateFriendRequestRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.friendship.FriendRequestResponse;
import iuh.fit.goat.exception.ConflictException;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.NotFoundException;
import org.springframework.data.domain.Pageable;

public interface FriendRequestService {
        ResultPaginationResponse handleGetMyFriends(Pageable pageable, String searchTerm) throws InvalidException;

        ResultPaginationResponse handleGetMyReceivedFriendRequests(Pageable pageable) throws InvalidException;

        ResultPaginationResponse handleGetMySentFriendRequests(Pageable pageable) throws InvalidException;

        ResultPaginationResponse handleGetMyBlockedUsers(Pageable pageable) throws InvalidException;

    FriendRequestResponse handleCreateFriendRequest(CreateFriendRequestRequest request)
            throws InvalidException, ConflictException, NotFoundException;

    FriendRequestResponse handleAcceptFriendRequest(Long requestId)
            throws InvalidException, ConflictException, NotFoundException;

    FriendRequestResponse handleRejectFriendRequest(Long requestId)
            throws InvalidException, ConflictException, NotFoundException;

    FriendRequestResponse handleCancelFriendRequest(Long requestId)
            throws InvalidException, ConflictException, NotFoundException;

    FriendRequestResponse handleBlockUser(Long targetUserId)
            throws InvalidException, NotFoundException;

    FriendRequestResponse handleUnblockUser(Long targetUserId)
            throws InvalidException, NotFoundException;
}
