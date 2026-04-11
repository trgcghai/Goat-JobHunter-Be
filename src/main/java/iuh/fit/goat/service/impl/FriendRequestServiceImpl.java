package iuh.fit.goat.service.impl;

import iuh.fit.goat.component.realtime.friendship.FriendshipRealtimeEvent;
import iuh.fit.goat.dto.request.friendship.CreateFriendRequestRequest;
import iuh.fit.goat.dto.response.friendship.FriendRequestResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.FriendRequest;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.entity.UserRelationship;
import iuh.fit.goat.enumeration.FriendRequestStatus;
import iuh.fit.goat.enumeration.FriendshipRealtimeEventType;
import iuh.fit.goat.enumeration.RelationshipState;
import iuh.fit.goat.exception.ConflictException;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.NotFoundException;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.repository.FriendRequestRepository;
import iuh.fit.goat.repository.UserRelationshipRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.FriendRequestService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FriendRequestServiceImpl implements FriendRequestService {
    private final FriendRequestRepository friendRequestRepository;
    private final UserRelationshipRepository userRelationshipRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public FriendRequestResponse handleCreateFriendRequest(CreateFriendRequestRequest request)
            throws InvalidException, ConflictException, NotFoundException {
        if (request == null || request.getTargetUserId() == null || request.getTargetUserId() <= 0) {
            throw new InvalidException("Target user ID is invalid");
        }

        User sender = this.handleGetCurrentUser();
        if (Objects.equals(sender.getAccountId(), request.getTargetUserId())) {
            throw new InvalidException("You cannot send a friend request to yourself");
        }

        User receiver = this.userRepository.findByAccountIdAndDeletedAtIsNull(request.getTargetUserId())
                .orElseThrow(() -> new NotFoundException("Target user doesn't exist"));

        UserPair pair = UserPair.of(sender, receiver);

        Optional<UserRelationship> existingRelationship = this.userRelationshipRepository
                .findByPairForUpdate(pair.pairLowId(), pair.pairHighId());

        if (existingRelationship.isPresent()) {
            if (existingRelationship.get().getRelationshipState() == RelationshipState.FRIEND) {
                throw new ConflictException("Users are already friends");
            }
            if (existingRelationship.get().getRelationshipState() == RelationshipState.BLOCKED) {
                throw new ConflictException("Cannot send friend request because this pair is blocked");
            }
        }

        boolean hasPending = this.friendRequestRepository
            .existsByPairLowUser_AccountIdAndPairHighUser_AccountIdAndStatusAndDeletedAtIsNull(
                        pair.pairLowId(),
                        pair.pairHighId(),
                        FriendRequestStatus.PENDING
                );
        if (hasPending) {
            throw new ConflictException("A pending friend request already exists for this pair");
        }

        Instant now = Instant.now();
        FriendRequest entity = new FriendRequest();
        entity.setSender(sender);
        entity.setReceiver(receiver);
        entity.setPairLowUser(pair.pairLowUser());
        entity.setPairHighUser(pair.pairHighUser());
        entity.setStatus(FriendRequestStatus.PENDING);
        entity.setRequestedAt(now);

        FriendRequest saved;
        try {
            saved = this.friendRequestRepository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("A pending friend request already exists for this pair");
        }

        this.publishRealtimeEvent(
                FriendshipRealtimeEventType.FRIEND_REQUEST_CREATED,
                sender,
                receiver,
                saved.getRequestId(),
                null
        );

        return this.convertToResponse(saved, null);
    }

    @Override
    @Transactional
    public FriendRequestResponse handleAcceptFriendRequest(Long requestId)
            throws InvalidException, ConflictException, NotFoundException {
        this.validateRequestId(requestId);

        User currentUser = this.handleGetCurrentUser();
        FriendRequest request = this.handleGetPendingRequestForUpdate(requestId);

        if (!Objects.equals(request.getReceiver().getAccountId(), currentUser.getAccountId())) {
            throw new InvalidException("Only the receiver can accept this friend request");
        }

        UserPair pair = UserPair.of(request.getSender(), request.getReceiver());

        Optional<UserRelationship> existingRelationship = this.userRelationshipRepository
                .findByPairForUpdate(pair.pairLowId(), pair.pairHighId());

        if (existingRelationship.isPresent()) {
            if (existingRelationship.get().getRelationshipState() == RelationshipState.BLOCKED) {
                throw new ConflictException("Cannot accept friend request because this pair is blocked");
            }
            if (existingRelationship.get().getRelationshipState() == RelationshipState.FRIEND) {
                throw new ConflictException("Users are already friends");
            }
        }

        Instant now = Instant.now();
        request.setStatus(FriendRequestStatus.ACCEPTED);
        request.setRespondedAt(now);
        FriendRequest saved = this.friendRequestRepository.save(request);

        this.userRelationshipRepository.upsertFriendRelationship(
                pair.pairLowId(),
                pair.pairHighId(),
                RelationshipState.FRIEND.name(),
                now,
                currentUser.getEmail()
        );

        this.friendRequestRepository.updateStatusForOtherRequests(
                pair.pairLowId(),
                pair.pairHighId(),
                FriendRequestStatus.PENDING,
                FriendRequestStatus.CANCELED,
                now,
                saved.getRequestId()
        );

        this.publishRealtimeEvent(
                FriendshipRealtimeEventType.FRIEND_REQUEST_ACCEPTED,
                currentUser,
                request.getSender(),
                saved.getRequestId(),
                RelationshipState.FRIEND
        );

        return this.convertToResponse(saved, RelationshipState.FRIEND);
    }

    @Override
    @Transactional
    public FriendRequestResponse handleRejectFriendRequest(Long requestId)
            throws InvalidException, ConflictException, NotFoundException {
        this.validateRequestId(requestId);

        User currentUser = this.handleGetCurrentUser();
        FriendRequest request = this.handleGetPendingRequestForUpdate(requestId);

        if (!Objects.equals(request.getReceiver().getAccountId(), currentUser.getAccountId())) {
            throw new InvalidException("Only the receiver can reject this friend request");
        }

        Instant now = Instant.now();
        request.setStatus(FriendRequestStatus.REJECTED);
        request.setRespondedAt(now);
        FriendRequest saved = this.friendRequestRepository.save(request);

        this.publishRealtimeEvent(
                FriendshipRealtimeEventType.FRIEND_REQUEST_REJECTED,
                currentUser,
                request.getSender(),
                saved.getRequestId(),
                null
        );

        return this.convertToResponse(saved, null);
    }

    @Override
    @Transactional
    public FriendRequestResponse handleCancelFriendRequest(Long requestId)
            throws InvalidException, ConflictException, NotFoundException {
        this.validateRequestId(requestId);

        User currentUser = this.handleGetCurrentUser();
        FriendRequest request = this.handleGetPendingRequestForUpdate(requestId);

        if (!Objects.equals(request.getSender().getAccountId(), currentUser.getAccountId())) {
            throw new InvalidException("Only the sender can cancel this friend request");
        }

        Instant now = Instant.now();
        request.setStatus(FriendRequestStatus.CANCELED);
        request.setRespondedAt(now);
        FriendRequest saved = this.friendRequestRepository.save(request);

        this.publishRealtimeEvent(
                FriendshipRealtimeEventType.FRIEND_REQUEST_CANCELED,
                currentUser,
                request.getReceiver(),
                saved.getRequestId(),
                null
        );

        return this.convertToResponse(saved, null);
    }

    private User handleGetCurrentUser() throws InvalidException {
        String currentEmail = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new InvalidException("User not authenticated")
        );

        Account account = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail)
                .orElseThrow(() -> new InvalidException("Current account doesn't exist"));

        if (!(account instanceof User user)) {
            throw new InvalidException("Only user accounts can perform this action");
        }

        return user;
    }

    private FriendRequest handleGetPendingRequestForUpdate(Long requestId)
            throws NotFoundException, ConflictException {
        FriendRequest request = this.friendRequestRepository.findActiveByIdForUpdate(requestId)
                .orElseThrow(() -> new NotFoundException("Friend request not found"));

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new ConflictException("Friend request is no longer pending");
        }

        return request;
    }

    private void validateRequestId(Long requestId) throws InvalidException {
        if (requestId == null || requestId <= 0) {
            throw new InvalidException("Request ID is invalid");
        }
    }

    private FriendRequestResponse convertToResponse(FriendRequest request, RelationshipState relationshipState) {
        FriendRequestResponse response = new FriendRequestResponse();
        response.setRequestId(request.getRequestId());
        response.setSenderId(request.getSender().getAccountId());
        response.setReceiverId(request.getReceiver().getAccountId());
        response.setStatus(request.getStatus());
        response.setRelationshipState(relationshipState);
        response.setRequestedAt(request.getRequestedAt() != null ? request.getRequestedAt() : request.getCreatedAt());
        response.setRespondedAt(request.getRespondedAt());
        return response;
    }

    private void publishRealtimeEvent(
            FriendshipRealtimeEventType type,
            User actor,
            User target,
            Long requestId,
            RelationshipState relationshipState
    ) {
        this.eventPublisher.publishEvent(
                FriendshipRealtimeEvent.builder()
                        .type(type)
                        .actorUserId(actor.getAccountId())
                        .targetUserId(target.getAccountId())
                        .requestId(requestId)
                        .relationshipState(relationshipState)
                        .actorPrincipal(actor.getEmail())
                        .targetPrincipal(target.getEmail())
                        .build()
        );
    }

    private record UserPair(User pairLowUser, User pairHighUser) {
        private static UserPair of(User firstUser, User secondUser) {
            return firstUser.getAccountId() <= secondUser.getAccountId()
                    ? new UserPair(firstUser, secondUser)
                    : new UserPair(secondUser, firstUser);
        }

        private Long pairLowId() {
            return this.pairLowUser.getAccountId();
        }

        private Long pairHighId() {
            return this.pairHighUser.getAccountId();
        }
    }
}
