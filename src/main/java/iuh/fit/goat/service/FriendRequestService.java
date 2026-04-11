package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.friendship.CreateFriendRequestRequest;
import iuh.fit.goat.dto.response.friendship.FriendRequestResponse;
import iuh.fit.goat.exception.ConflictException;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.NotFoundException;

public interface FriendRequestService {
    FriendRequestResponse handleCreateFriendRequest(CreateFriendRequestRequest request)
            throws InvalidException, ConflictException, NotFoundException;

    FriendRequestResponse handleAcceptFriendRequest(Long requestId)
            throws InvalidException, ConflictException, NotFoundException;

    FriendRequestResponse handleRejectFriendRequest(Long requestId)
            throws InvalidException, ConflictException, NotFoundException;

    FriendRequestResponse handleCancelFriendRequest(Long requestId)
            throws InvalidException, ConflictException, NotFoundException;
}
