package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.Role;
import iuh.fit.goat.common.MessageEvent;
import iuh.fit.goat.dto.request.message.ForwardMessageRequest;
import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.dto.response.message.ForwardMessageFailureResponse;
import iuh.fit.goat.dto.response.message.ForwardMessageResponse;
import iuh.fit.goat.dto.response.message.ForwardMessageSuccessResponse;
import iuh.fit.goat.dto.response.message.MessageDeletedEventResponse;
import iuh.fit.goat.dto.response.message.MessageResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.StorageResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.entity.embeddable.MediaItem;
import iuh.fit.goat.entity.embeddable.SenderInfo;
import iuh.fit.goat.enumeration.ChatRoomType;
import iuh.fit.goat.enumeration.MediaType;
import iuh.fit.goat.enumeration.MessageType;
import iuh.fit.goat.enumeration.RelationshipState;
import iuh.fit.goat.exception.BlockedInteractionException;
import iuh.fit.goat.exception.ConflictException;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.NotFoundException;
import iuh.fit.goat.exception.PermissionException;
import iuh.fit.goat.repository.ChatMemberRepository;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.MessageHiddenRepository;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.repository.UserRelationshipRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.MessageService;
import iuh.fit.goat.service.StorageService;
import iuh.fit.goat.util.MessageHelper;
import iuh.fit.goat.util.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private final StorageService storageService;

    private final MessageHiddenRepository messageHiddenRepository;
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRelationshipRepository userRelationshipRepository;
    private final UserRepository userRepository;

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final int MAX_MEDIA_ITEMS_PER_MESSAGE = 10;
    private static final long MAX_MEDIA_BATCH_SIZE = 100 * 1024 * 1024; // 100MB
    private static final int MAX_FORWARD_TARGETS = 20;
    private static final String MESSAGE_DELETED_EVENT = "MESSAGE_DELETED";
    private static final String DELETE_TYPE_ORIGINAL = "original";
    private static final String DELETE_TYPE_FORWARDED = "forwarded";
    private static final int MAX_REPLY_PREVIEW_LENGTH = 120;
    private static final String REVOKED_MESSAGE_PREVIEW = "Tin nhắn đã được thu hồi";
    private static final String UNAVAILABLE_MESSAGE_PREVIEW = "Tin nhắn không khả dụng";
    private static final int DEFAULT_SEARCH_PAGE_SIZE = 20;
    private static final int MAX_SEARCH_PAGE_SIZE = 50;
    private static final int MAX_SEARCH_TERM_LENGTH = 100;
    private static final int SEARCH_SCAN_LIMIT = 3000;
    private static final int HIDDEN_FILTER_FETCH_MULTIPLIER = 3;
    private static final int HIDDEN_FILTER_MAX_FETCH_ROUNDS = 4;
    private static final int MAX_MESSAGE_FETCH_SIZE = 500;
    private static final int MAX_SEARCH_TOP_UP_ROUNDS = 2;
    private final SimpMessagingTemplate messagingTemplate;

    // ========== PUBLIC API METHODS ==========

    /**
    * Get last message in chat room.
     */
    @Override
    public Message getLastMessageByChatRoom(Long chatRoomId) throws InvalidException {
        if (chatRoomId == null) {
            throw new InvalidException("Chat room ID cannot be null");
        }

        this.chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new InvalidException("Chat Room not found"));

        Optional<Message> lastMessage = this.messageRepository
                .findLastMessageByConversation(chatRoomId.toString());

        if (lastMessage.isEmpty()) {
            log.warn("No messages found for chatRoom: {}", chatRoomId);
        }

        return lastMessage.orElse(null);
    }

    /**
    * Get messages sorted by newest first.
     */
    @Override
    public List<Message> getMessagesByChatRoom(Long chatRoomId, Pageable pageable, Account currentAccount) {
        if (chatRoomId == null) {
            throw new IllegalArgumentException("Chat room ID cannot be null");
        }
        if (currentAccount == null) {
            throw new IllegalArgumentException("Current account is invalid");
        }

        int requestedSize = resolveRequestedSize(pageable, DEFAULT_SEARCH_PAGE_SIZE);

        log.info("Fetching up to {} messages for chatRoom: {}", requestedSize, chatRoomId);

        List<Message> messages = fetchMessagesByVisibility(
                chatRoomId.toString(),
                requestedSize,
                true,
                currentAccount.getAccountId(),
                null
        );

        log.info("Retrieved {} visible messages for chatRoom: {}, accountId={}",
                messages.size(),
                chatRoomId,
                currentAccount.getAccountId());

        return messages;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationResponse searchMessagesByChatRoom(
            Long chatRoomId,
            String searchTerm,
            Pageable pageable,
            Account currentAccount
    )
            throws InvalidException {
        if (chatRoomId == null) {
            throw new InvalidException("Chat room ID cannot be null");
        }
        if (currentAccount == null) {
            throw new InvalidException("Current account is invalid");
        }

        this.chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new InvalidException("Chat Room not found"));

        int pageNumber = pageable != null ? Math.max(pageable.getPageNumber(), 0) : 0;
        int requestedPageSize = pageable != null ? pageable.getPageSize() : DEFAULT_SEARCH_PAGE_SIZE;
        int pageSize = resolveSearchPageSize(requestedPageSize);

        String normalizedSearchTerm = normalizeSearchTerm(searchTerm);
        if (normalizedSearchTerm == null) {
            return buildSearchPaginationResponse(Collections.emptyList(), pageNumber, pageSize, 0);
        }

        MessageRepository.MessageSearchResult searchResult = this.messageRepository.searchMessagesByChatRoom(
                chatRoomId.toString(),
                normalizedSearchTerm,
                pageNumber,
                pageSize,
                SEARCH_SCAN_LIMIT
        );

        if (searchResult.scanLimitReached()) {
            log.warn("Message search reached scan limit: chatRoomId={}, page={}, pageSize={}, scanLimit={}",
                    chatRoomId,
                    pageNumber + 1,
                    pageSize,
                    SEARCH_SCAN_LIMIT);
        }

        List<Message> visibleMessages = new ArrayList<>(
                filterHiddenMessagesForUser(searchResult.messages(), currentAccount.getAccountId())
        );

        int nextPage = pageNumber + 1;
        int topUpRound = 0;
        while (visibleMessages.size() < pageSize
                && topUpRound < MAX_SEARCH_TOP_UP_ROUNDS
                && searchResult.messages().size() >= pageSize) {
            MessageRepository.MessageSearchResult nextPageResult = this.messageRepository.searchMessagesByChatRoom(
                    chatRoomId.toString(),
                    normalizedSearchTerm,
                    nextPage,
                    pageSize,
                    SEARCH_SCAN_LIMIT
            );

            if (nextPageResult.messages().isEmpty()) {
                break;
            }

            visibleMessages.addAll(filterHiddenMessagesForUser(nextPageResult.messages(), currentAccount.getAccountId()));

            if (nextPageResult.messages().size() < pageSize) {
                break;
            }

            nextPage++;
            topUpRound++;
        }

        List<Message> pageMessages = visibleMessages.size() > pageSize
                ? new ArrayList<>(visibleMessages.subList(0, pageSize))
                : visibleMessages;

        List<MessageResponse> result = toMessageResponses(pageMessages);
        return buildSearchPaginationResponse(result, pageNumber, pageSize, searchResult.matchedCount());
    }

    /**
    * Send text message.
     */
    @Override
    @Transactional
    public Message sendMessage(Long chatRoomId, MessageCreateRequest request, Account currentAccount) throws InvalidException
    {
        ChatRoom chatRoom = this.chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new InvalidException("Chat Room not found"));
        this.validateNoBlockedDirectInteraction(chatRoom, currentAccount.getAccountId());

        String replyToMessageId = normalizeReplyToMessageId(request != null ? request.getReplyToMessageId() : null);
        validateReplyTarget(chatRoomId.toString(), replyToMessageId);

        String messageId = generateMessageId();
        Instant now = Instant.now();
        long timestamp = now.toEpochMilli();

        String messageSk = Message.buildMessageSk(timestamp, messageId);

        // Build sender information
        SenderInfo senderInfo = buildSenderInfo(currentAccount);

        Message message = Message.builder()
                .messageSk(messageSk)
                .chatRoomId(chatRoomId.toString())
                .messageId(messageId)
                .sender(senderInfo)  // NEW: Use embedded sender
                .content(request.getContent())
                .messageType(MessageType.TEXT)
                .replyTo(replyToMessageId)
                .isHidden(false)
                .isForwarded(false)
                .originalMessageId(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        log.info("Saving message - chatRoomId: {}, SK: {}", chatRoomId, messageSk);

        Message savedMessage = messageRepository.saveMessage(message);

        log.info("Message created: messageId={}, chatRoomId={}",
            messageId, chatRoomId);

        sendMessageToUsers(chatRoomId, savedMessage);

        return savedMessage;
    }

    /**
     * Send messages with files (batch operation)
     */
    @Override
    @Transactional
    public List<Message> sendMessagesWithFiles(
            Long chatRoomId,
            MessageCreateRequest request,
            List<MultipartFile> files,
            Account currentAccount
    ) throws InvalidException {
        ChatRoom chatRoom = this.chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new InvalidException("Chat Room not found"));
        this.validateNoBlockedDirectInteraction(chatRoom, currentAccount.getAccountId());

        String replyToMessageId = normalizeReplyToMessageId(request != null ? request.getReplyToMessageId() : null);
        validateReplyTarget(chatRoomId.toString(), replyToMessageId);
        String normalizedContent = normalizeMessageContent(request != null ? request.getContent() : null);

        List<Message> createdMessages = new ArrayList<>();
        boolean mediaMessageCreated = false;
        try {
            // Process files first
            if (files != null && !files.isEmpty()) {
                List<MultipartFile> mediaFiles = new ArrayList<>();
                List<MultipartFile> nonMediaFiles = new ArrayList<>();

                for (MultipartFile file : files) {
                    validateFile(file);

                    MediaType mediaType = determineMediaType(file.getContentType());
                    if (mediaType != null) {
                        mediaFiles.add(file);
                    } else {
                        nonMediaFiles.add(file);
                    }
                }

                if (!mediaFiles.isEmpty()) {
                    validateMediaBatchConstraints(mediaFiles);

                    if (mediaFiles.size() >= 2) {
                        List<MediaItem> mediaItems = new ArrayList<>();
                        int displayOrder = 0;

                        for (MultipartFile mediaFile : mediaFiles) {
                            String mimeType = mediaFile.getContentType();
                            MediaType mediaType = determineMediaType(mimeType);
                            if (mediaType == null) {
                                continue;
                            }

                            String folder = getFolderByMediaType(mediaType);
                            StorageResponse storageResponse = storageService.handleUploadFile(mediaFile, folder);

                            mediaItems.add(MediaItem.builder()
                                    .url(storageResponse.getUrl())
                                    .mediaType(mediaType)
                                    .mimeType(mimeType)
                                    .sizeBytes(mediaFile.getSize())
                                    .displayOrder(displayOrder++)
                                    .build());
                        }

                        if (!mediaItems.isEmpty()) {
                            Message mediaMessage = createMediaMessage(
                                    chatRoomId.toString(),
                                    mediaItems,
                                    normalizedContent,
                                    replyToMessageId,
                                    currentAccount
                            );

                            Message savedMessage = messageRepository.saveMessage(mediaMessage);
                            createdMessages.add(savedMessage);
                            sendMessageToUsers(chatRoomId, savedMessage);

                            mediaMessageCreated = true;
                            log.info("Media batch message created: messageId={}, mediaItems={}, chatRoomId={}",
                                    savedMessage.getMessageId(),
                                    mediaItems.size(),
                                    chatRoomId);
                        }
                    } else {
                        MultipartFile mediaFile = mediaFiles.get(0);
                        MessageType messageType = determineLegacyMediaMessageType(mediaFile.getContentType());

                        String folder = getFolderByMessageType(messageType);
                        StorageResponse storageResponse = storageService.handleUploadFile(mediaFile, folder);
                        String fileUrl = storageResponse.getUrl();

                        Message fileMessage = createFileMessage(
                                chatRoomId.toString(),
                                fileUrl,
                                messageType,
                                replyToMessageId,
                                currentAccount
                        );

                        Message savedMessage = messageRepository.saveMessage(fileMessage);
                        createdMessages.add(savedMessage);
                        sendMessageToUsers(chatRoomId, savedMessage);

                        log.info("Single media message created in legacy format: messageId={}, type={}, chatRoomId={}",
                                savedMessage.getMessageId(),
                                messageType,
                                chatRoomId);
                    }
                }

                for (MultipartFile file : nonMediaFiles) {
                    MessageType messageType = determineMessageType(file.getContentType());

                    String folder = getFolderByMessageType(messageType);
                    StorageResponse storageResponse = storageService.handleUploadFile(file, folder);
                    String fileUrl = storageResponse.getUrl();

                    Message fileMessage = createFileMessage(
                            chatRoomId.toString(),
                            fileUrl,
                            messageType,
                            replyToMessageId,
                            currentAccount
                    );

                    Message savedMessage = messageRepository.saveMessage(fileMessage);
                    createdMessages.add(savedMessage);
                    sendMessageToUsers(chatRoomId, savedMessage);

                    log.info("File message created: messageId={}, type={}, chatRoomId={}",
                            savedMessage.getMessageId(), messageType, chatRoomId);
                }
            }

            // Process text content
            if (!mediaMessageCreated && normalizedContent != null) {
                MessageCreateRequest textRequest = new MessageCreateRequest(
                        normalizedContent,
                        replyToMessageId
                );
                Message textMessage = sendMessage(chatRoomId, textRequest, currentAccount);
                createdMessages.add(textMessage);
            }

            if (createdMessages.isEmpty()) {
                throw new InvalidException("At least one file or text content is required");
            }

            log.info("Batch message creation completed: {} messages created in chatRoom {}",
                    createdMessages.size(), chatRoomId);

            return createdMessages;

        } catch (BlockedInteractionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating messages", e);
            throw new InvalidException("Failed to create messages: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<Message> sendContactCardMessages(Long chatRoomId, List<Long> userIds, Account currentAccount)
            throws InvalidException {
        if (chatRoomId == null) {
            throw new InvalidException("Chat room ID cannot be null");
        }
        if (currentAccount == null) {
            throw new InvalidException("Current account is invalid");
        }

        ChatRoom chatRoom = this.chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new InvalidException("Chat Room not found"));
        this.validateNoBlockedDirectInteraction(chatRoom, currentAccount.getAccountId());

        List<Long> normalizedUserIds = this.normalizeContactCardUserIds(userIds);
        if (normalizedUserIds.isEmpty()) {
            throw new InvalidException("At least one valid user ID is required");
        }

        List<Message> createdMessages = new ArrayList<>();
        for (Long userId : normalizedUserIds) {
            if (Objects.equals(userId, currentAccount.getAccountId())) {
                continue;
            }

            Optional<User> referencedUser = this.userRepository.findByAccountIdAndDeletedAtIsNull(userId);
            if (referencedUser.isEmpty()) {
                continue;
            }

            if (!this.isFriendWithCurrentUser(currentAccount.getAccountId(), userId)) {
                continue;
            }

            Message contactCardMessage = this.createContactCardMessage(chatRoomId.toString(), userId, currentAccount);
            Message savedMessage = this.messageRepository.saveMessage(contactCardMessage);
            createdMessages.add(savedMessage);
            sendMessageToUsers(chatRoomId, savedMessage);
        }

        if (createdMessages.isEmpty()) {
            throw new InvalidException("No eligible contacts to send");
        }

        log.info("CONTACT_CARD batch completed: {} cards sent in chatRoom {}", createdMessages.size(), chatRoomId);
        return createdMessages;
    }

    @Override
    public void sendMessageToUsers(Long chatRoomId, Message message) {
        if (chatRoomId == null) {
            log.warn("Skip realtime emit because chatRoomId is null for messageId={}",
                    message != null ? message.getMessageId() : null);
            return;
        }

        sendMessageToUsers(chatRoomId.toString(), message);
    }

    @Override
    public MessageResponse toMessageResponse(Message message) {
        return toMessageResponse(message, Collections.emptyMap(), new HashMap<>(), new HashMap<>());
    }

    @Override
    public List<MessageResponse> toMessageResponses(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Message> localMessages = new HashMap<>();
        for (Message message : messages) {
            if (message == null || message.getMessageId() == null || message.getMessageId().isBlank()) {
                continue;
            }
            localMessages.putIfAbsent(message.getMessageId(), message);
        }

        Map<String, Optional<Message>> parentLookupCache = new HashMap<>();
        Map<Long, Optional<User>> contactLookupCache = preloadContactUsers(messages);
        List<MessageResponse> responses = new ArrayList<>(messages.size());
        for (Message message : messages) {
            responses.add(toMessageResponse(message, localMessages, parentLookupCache, contactLookupCache));
        }

        return responses;
    }

    @Override
    public List<Message> getMediaMessagesByChatRoom(Long chatRoomId, Pageable pageable, Account currentAccount)
            throws InvalidException {
        if (chatRoomId == null) {
            throw new InvalidException("Chat room ID cannot be null");
        }
        if (currentAccount == null) {
            throw new InvalidException("Current account is invalid");
        }

        chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new InvalidException("Chat Room not found"));

        int requestedSize = resolveRequestedSize(pageable, DEFAULT_SEARCH_PAGE_SIZE);

        log.info("Fetching up to {} media messages for chatRoom: {}", requestedSize, chatRoomId);

        List<Message> mediaMessages = fetchMessagesByVisibility(
                chatRoomId.toString(),
                requestedSize,
                false,
                currentAccount.getAccountId(),
                message -> message != null && isMediaType(message.getMessageType())
        );

        log.info("Retrieved {} media messages for chatRoom: {}, accountId={}",
                mediaMessages.size(),
                chatRoomId,
                currentAccount.getAccountId());

        return mediaMessages;
    }

    @Override
    public List<Message> getFileMessagesByChatRoom(Long chatRoomId, Pageable pageable, Account currentAccount)
            throws InvalidException {
        if (chatRoomId == null) {
            throw new InvalidException("Chat room ID cannot be null");
        }
        if (currentAccount == null) {
            throw new InvalidException("Current account is invalid");
        }

        chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new InvalidException("Chat Room not found"));

        int requestedSize = resolveRequestedSize(pageable, DEFAULT_SEARCH_PAGE_SIZE);

        log.info("Fetching up to {} file messages for chatRoom: {}", requestedSize, chatRoomId);

        List<Message> fileMessages = fetchMessagesByVisibility(
                chatRoomId.toString(),
                requestedSize,
                false,
                currentAccount.getAccountId(),
                message -> message != null && message.getMessageType() == MessageType.FILE
        );

        log.info("Retrieved {} file messages for chatRoom: {}, accountId={}",
                fileMessages.size(),
                chatRoomId,
                currentAccount.getAccountId());

        return fileMessages;
    }

    @Override
    @Transactional
    public void hideMessageForMe(Long chatRoomId, String messageId, Account currentAccount)
            throws InvalidException, NotFoundException, PermissionException {
        if (chatRoomId == null) {
            throw new InvalidException("Chat room ID cannot be null");
        }
        if (messageId == null || messageId.isBlank()) {
            throw new InvalidException("Message ID cannot be blank");
        }
        if (currentAccount == null) {
            throw new InvalidException("Current account is invalid");
        }

        this.chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new NotFoundException("Chat room not found"));

        if (!isUserInChatRoom(chatRoomId, currentAccount.getAccountId())) {
            throw new PermissionException("Current user is not a member of this chat room");
        }

        Message message = this.messageRepository
                .findByChatRoomIdAndMessageId(chatRoomId.toString(), messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));

        this.messageHiddenRepository.hideMessageForUser(
                message.getMessageId(),
                currentAccount.getAccountId(),
                Instant.now()
        );

        log.info("Message hidden for current user: chatRoomId={}, messageId={}, accountId={}",
                chatRoomId,
                messageId,
                currentAccount.getAccountId());
    }

    @Override
    public Message revokeMessage(Long chatRoomId, String messageId, Account currentAccount)
            throws InvalidException, NotFoundException, ConflictException, PermissionException {
        if (chatRoomId == null) {
            throw new InvalidException("Chat room ID cannot be null");
        }
        if (messageId == null || messageId.isBlank()) {
            throw new InvalidException("Message ID cannot be blank");
        }
        if (currentAccount == null) {
            throw new InvalidException("Current account is invalid");
        }

        this.chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new NotFoundException("Chat room not found"));

        Message message = this.messageRepository
                .findByChatRoomIdAndMessageId(chatRoomId.toString(), messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));

        if (Boolean.TRUE.equals(message.getIsHidden())) {
            throw new ConflictException("Message has already been revoked");
        }

        Long senderAccountId = extractSenderAccountId(message);
        if (senderAccountId == null) {
            throw new InvalidException("Message sender information is missing");
        }

        if (!Objects.equals(senderAccountId, currentAccount.getAccountId())) {
            throw new PermissionException("Only sender can revoke this message");
        }

        List<Message> cascadeMessages = collectCascadeRecallMessages(message);

        List<Message> recalledMessages;
        try {
            recalledMessages = applyRecallState(cascadeMessages);
        } catch (RuntimeException e) {
            log.error("Failed to cascade revoke messageId={} in chatRoomId={}", messageId, chatRoomId, e);
            throw new InvalidException("Failed to revoke message");
        }

        LinkedHashSet<String> affectedChatRoomIds = new LinkedHashSet<>();
        for (Message recalledMessage : recalledMessages) {
            if (recalledMessage.getChatRoomId() != null && !recalledMessage.getChatRoomId().isBlank()) {
                affectedChatRoomIds.add(recalledMessage.getChatRoomId());
            }

            sendMessageToUsers(recalledMessage.getChatRoomId(), recalledMessage);
        }

        Message revokedMessage = recalledMessages.stream()
                .filter(recalledMessage -> messageId.equals(recalledMessage.getMessageId()))
                .findFirst()
                .orElse(message);

        log.info("Cascade message revoke completed: messageId={}, rootChatRoomId={}, byAccountId={}, totalCandidates={}, totalRecalled={}, affectedChatRooms={}",
                messageId,
                chatRoomId,
                currentAccount.getAccountId(),
                cascadeMessages.size(),
                recalledMessages.size(),
                affectedChatRoomIds);

        return revokedMessage;
    }

    @Override
    public ForwardMessageResponse forwardMessage(
            Long sourceChatRoomId,
            String messageId,
            ForwardMessageRequest request,
            Account currentAccount
    ) throws InvalidException, NotFoundException, PermissionException {
        if (sourceChatRoomId == null) {
            throw new InvalidException("Source chat room ID cannot be null");
        }
        if (messageId == null || messageId.isBlank()) {
            throw new InvalidException("Message ID cannot be blank");
        }
        if (currentAccount == null) {
            throw new InvalidException("Current account is invalid");
        }

        List<Long> targetChatRoomIds = normalizeTargetChatRoomIds(request);

        this.chatRoomRepository.findById(sourceChatRoomId)
                .orElseThrow(() -> new NotFoundException("Source chat room not found"));

        if (!isUserInChatRoom(sourceChatRoomId, currentAccount.getAccountId())) {
            throw new PermissionException("Current user is not a member of source chat room");
        }

        Message sourceMessage = this.messageRepository
                .findByChatRoomIdAndMessageId(sourceChatRoomId.toString(), messageId)
                .orElseThrow(() -> new NotFoundException("Source message not found"));

        validateForwardableSourceMessage(sourceMessage);

        List<ForwardMessageSuccessResponse> successes = new ArrayList<>();
        List<ForwardMessageFailureResponse> failures = new ArrayList<>();

        for (Long targetChatRoomId : targetChatRoomIds) {
            if (Objects.equals(targetChatRoomId, sourceChatRoomId)) {
                failures.add(buildForwardFailure(
                        targetChatRoomId,
                        "TARGET_EQUALS_SOURCE",
                        "Cannot forward message to source chat room"
                ));
                continue;
            }

            if (this.chatRoomRepository.findById(targetChatRoomId).isEmpty()) {
                failures.add(buildForwardFailure(
                        targetChatRoomId,
                        "TARGET_NOT_FOUND",
                        "Target chat room not found"
                ));
                continue;
            }

            if (!isUserInChatRoom(targetChatRoomId, currentAccount.getAccountId())) {
                failures.add(buildForwardFailure(
                        targetChatRoomId,
                        "TARGET_FORBIDDEN",
                        "Current user is not a member of target chat room"
                ));
                continue;
            }

            try {
                Message forwardedMessage = createForwardedMessage(sourceMessage, targetChatRoomId, currentAccount);
                Message savedMessage = this.messageRepository.saveMessage(forwardedMessage);
                sendMessageToUsers(targetChatRoomId, savedMessage);

                successes.add(ForwardMessageSuccessResponse.builder()
                        .targetChatRoomId(targetChatRoomId)
                        .message(toMessageResponse(savedMessage))
                        .build());
            } catch (RuntimeException e) {
                log.error("Failed to forward messageId={} to chatRoomId={}", messageId, targetChatRoomId, e);
                failures.add(buildForwardFailure(
                        targetChatRoomId,
                        "FORWARD_FAILED",
                        "Failed to forward message"
                ));
            }
        }

        log.info("Forward message completed: sourceChatRoomId={}, messageId={}, success={}, failed={}",
                sourceChatRoomId, messageId, successes.size(), failures.size());

        return ForwardMessageResponse.builder()
                .sourceChatRoomId(sourceChatRoomId)
                .sourceMessageId(messageId)
                .totalTargets(targetChatRoomIds.size())
                .successCount(successes.size())
                .failedCount(failures.size())
                .successes(successes)
                .failures(failures)
                .build();
    }

    @Override
    @Transactional
    public MessageDeletedEventResponse deleteMessagePermanently(Long chatRoomId, String messageId, Account currentAccount)
            throws InvalidException, NotFoundException, PermissionException {
        if (chatRoomId == null) {
            throw new InvalidException("Chat room ID cannot be null");
        }
        if (messageId == null || messageId.isBlank()) {
            throw new InvalidException("Message ID cannot be blank");
        }
        if (currentAccount == null) {
            throw new InvalidException("Current account is invalid");
        }

        this.chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new NotFoundException("Chat room not found"));

        Message message = this.messageRepository
                .findByChatRoomIdAndMessageId(chatRoomId.toString(), messageId)
                .orElseThrow(() -> new NotFoundException("Message not found or already permanently deleted"));

        Long senderAccountId = extractSenderAccountId(message);
        boolean canDeleteAsAdmin = isSuperAdmin(currentAccount);
        boolean canDeleteAsSender = senderAccountId != null && Objects.equals(senderAccountId, currentAccount.getAccountId());

        if (!canDeleteAsAdmin && !canDeleteAsSender) {
            throw new PermissionException("Only sender or super admin can permanently delete this message");
        }

        List<CascadeDeleteTarget> deleteTargets = collectCascadeDeleteTargets(message);
        List<MessageDeletedEventResponse> emittedEvents = new ArrayList<>();

        for (CascadeDeleteTarget target : deleteTargets) {
            Message targetMessage = target.message();

            try {
                deleteSingleMessagePermanently(targetMessage);
            } catch (InvalidException e) {
                log.error("Cascade permanent delete failed: rootMessageId={}, rootChatRoomId={}, failedMessageId={}, failedChatRoomId={}, totalCandidates={}, totalDeleted={}",
                        messageId,
                        chatRoomId,
                        targetMessage != null ? targetMessage.getMessageId() : null,
                        targetMessage != null ? targetMessage.getChatRoomId() : null,
                        deleteTargets.size(),
                        emittedEvents.size(),
                        e);
                throw new InvalidException("Failed to permanently delete message");
            }

            MessageDeletedEventResponse event = buildMessageDeletedEvent(
                    targetMessage,
                    target.deleteType(),
                    currentAccount.getAccountId(),
                    Instant.now()
            );

            sendMessageDeletedEvent(targetMessage.getChatRoomId(), event);
            emittedEvents.add(event);
        }

        MessageDeletedEventResponse rootEvent = emittedEvents.stream()
                .filter(event -> messageId.equals(event.getMessageId()))
                .findFirst()
                .orElseThrow(() -> new InvalidException("Failed to permanently delete message"));

        LinkedHashSet<String> affectedChatRoomIds = new LinkedHashSet<>();
        for (MessageDeletedEventResponse event : emittedEvents) {
            if (event.getChatRoomId() != null && !event.getChatRoomId().isBlank()) {
                affectedChatRoomIds.add(event.getChatRoomId());
            }
        }

        log.info("Cascade permanent delete completed: messageId={}, rootChatRoomId={}, byAccountId={}, totalCandidates={}, totalDeleted={}, affectedChatRooms={}",
                messageId,
                chatRoomId,
                currentAccount.getAccountId(),
                deleteTargets.size(),
                emittedEvents.size(),
                affectedChatRoomIds);

        return rootEvent;
    }

    @Override
    public void createAndSendSystemMessage(
            Long chatRoomId,
            MessageEvent type,
            Account actor,
            Object... params
    ) {
        String content = MessageHelper.generateSystemMessage(type, actor, params);
        String messageId = generateMessageId();
        Instant now = Instant.now();
        long timestamp = now.toEpochMilli();

        String messageSk = Message.buildMessageSk(timestamp, messageId);

        SenderInfo senderInfo = buildSenderInfo(actor);

        Message systemMessage = Message.builder()
                .messageSk(messageSk)
                .chatRoomId(chatRoomId.toString())
                .messageId(messageId)
                .sender(senderInfo)
                .content(content)
                .messageType(MessageType.SYSTEM)
                .isHidden(false)
                .isForwarded(false)
                .originalMessageId(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Message savedMessage = messageRepository.saveMessage(systemMessage);
        sendMessageToUsers(chatRoomId, savedMessage);

        log.info("System message created: type={}, chatRoomId={}", type, chatRoomId);
    }

    private boolean isMediaType(MessageType type) {
        return type == MessageType.MEDIA
                || type == MessageType.IMAGE
                || type == MessageType.VIDEO
                || type == MessageType.AUDIO;
    }

    private ResultPaginationResponse buildSearchPaginationResponse(
            List<MessageResponse> messages,
            int pageNumber,
            int pageSize,
            long total
    ) {
        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageNumber + 1);
        meta.setPageSize(pageSize);
        meta.setPages(calculateTotalPages(total, pageSize));
        meta.setTotal(total);

        return new ResultPaginationResponse(meta, messages);
    }

    private int calculateTotalPages(long total, int pageSize) {
        if (total <= 0 || pageSize <= 0) {
            return 0;
        }

        return (int) ((total + pageSize - 1) / pageSize);
    }

    private int resolveSearchPageSize(int requestedPageSize) {
        if (requestedPageSize <= 0) {
            return DEFAULT_SEARCH_PAGE_SIZE;
        }

        return Math.min(requestedPageSize, MAX_SEARCH_PAGE_SIZE);
    }

    private int resolveRequestedSize(Pageable pageable, int fallbackSize) {
        if (pageable == null || pageable.getPageSize() <= 0) {
            return fallbackSize;
        }

        return pageable.getPageSize();
    }

    private List<Message> fetchMessagesByVisibility(
            String chatRoomId,
            int requestedSize,
            boolean includeGloballyHidden,
            Long accountId,
            Predicate<Message> messageFilter
    ) {
        int safeRequestedSize = Math.max(requestedSize, 1);
        int fetchLimit = Math.max(safeRequestedSize * HIDDEN_FILTER_FETCH_MULTIPLIER, safeRequestedSize);
        fetchLimit = Math.min(fetchLimit, MAX_MESSAGE_FETCH_SIZE);

        List<Message> visibleMessages = Collections.emptyList();

        for (int round = 0; round < HIDDEN_FILTER_MAX_FETCH_ROUNDS; round++) {
            List<Message> candidates = this.messageRepository.findMessagesByChatRoom(
                    chatRoomId,
                    fetchLimit,
                    includeGloballyHidden
            );

            List<Message> filteredCandidates = filterHiddenMessagesForUser(candidates, accountId);
            if (messageFilter != null) {
                filteredCandidates = filteredCandidates.stream()
                        .filter(Objects::nonNull)
                        .filter(messageFilter)
                        .toList();
            }

            visibleMessages = filteredCandidates;

            if (visibleMessages.size() >= safeRequestedSize) {
                break;
            }

            if (candidates.size() < fetchLimit || fetchLimit >= MAX_MESSAGE_FETCH_SIZE) {
                break;
            }

            fetchLimit = Math.min(fetchLimit * 2, MAX_MESSAGE_FETCH_SIZE);
        }

        if (visibleMessages.size() <= safeRequestedSize) {
            return visibleMessages;
        }

        return new ArrayList<>(visibleMessages.subList(0, safeRequestedSize));
    }

    private List<Message> filterHiddenMessagesForUser(List<Message> messages, Long accountId) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        if (accountId == null) {
            return messages;
        }

        List<String> messageIds = messages.stream()
                .map(Message::getMessageId)
                .filter(messageId -> messageId != null && !messageId.isBlank())
                .distinct()
                .toList();

        if (messageIds.isEmpty()) {
            return messages;
        }

        Set<String> hiddenMessageIds = this.messageHiddenRepository.findHiddenMessageIdsForUser(messageIds, accountId);
        if (hiddenMessageIds.isEmpty()) {
            return messages;
        }

        List<Message> visibleMessages = new ArrayList<>(messages.size());
        for (Message message : messages) {
            if (message == null) {
                continue;
            }

            String messageId = message.getMessageId();
            if (messageId == null || !hiddenMessageIds.contains(messageId)) {
                visibleMessages.add(message);
            }
        }

        return visibleMessages;
    }

    private String normalizeSearchTerm(String searchTerm) throws InvalidException {
        if (searchTerm == null) {
            return null;
        }

        String normalized = searchTerm.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.length() > MAX_SEARCH_TERM_LENGTH) {
            throw new InvalidException("searchTerm must not exceed " + MAX_SEARCH_TERM_LENGTH + " characters");
        }

        return normalized;
    }

    // ========== HELPER METHODS ==========

    private MessageResponse toMessageResponse(
            Message message,
            Map<String, Message> localMessages,
            Map<String, Optional<Message>> parentLookupCache,
            Map<Long, Optional<User>> contactLookupCache
    ) {
        if (message == null) {
            return null;
        }

        MessageResponse.ReplyContext replyContext = buildReplyContext(message, localMessages, parentLookupCache);
        MessageResponse.ContactCardContext contactCardContext = buildContactCardContext(message, contactLookupCache);
        return MessageMapper.toResponse(message, replyContext, contactCardContext);
    }

    private MessageResponse.ContactCardContext buildContactCardContext(
            Message message,
            Map<Long, Optional<User>> contactLookupCache
    ) {
        Long contactUserId = extractContactCardUserId(message);
        if (contactUserId == null) {
            return null;
        }

        User contactUser = resolveContactUser(contactUserId, contactLookupCache);
        if (contactUser == null) {
            return null;
        }

        return MessageResponse.ContactCardContext.builder()
                .accountId(contactUser.getAccountId())
                .fullName(contactUser.getFullName())
                .username(contactUser.getUsername())
                .avatar(contactUser.getAvatar())
                .headline(contactUser.getHeadline())
                .bio(contactUser.getBio())
                .coverPhoto(contactUser.getCoverPhoto())
                .visibility(contactUser.getVisibility())
                .build();
    }

    private Map<Long, Optional<User>> preloadContactUsers(List<Message> messages) {
        Map<Long, Optional<User>> contactLookupCache = new HashMap<>();
        if (messages == null || messages.isEmpty()) {
            return contactLookupCache;
        }

        LinkedHashSet<Long> contactUserIds = new LinkedHashSet<>();
        for (Message message : messages) {
            Long contactUserId = extractContactCardUserId(message);
            if (contactUserId != null) {
                contactUserIds.add(contactUserId);
            }
        }

        if (contactUserIds.isEmpty()) {
            return contactLookupCache;
        }

        List<User> existingUsers = this.userRepository
                .findByAccountIdInAndDeletedAtIsNull(new ArrayList<>(contactUserIds));

        for (User user : existingUsers) {
            contactLookupCache.put(user.getAccountId(), Optional.of(user));
        }

        for (Long userId : contactUserIds) {
            contactLookupCache.putIfAbsent(userId, Optional.empty());
        }

        return contactLookupCache;
    }

    private User resolveContactUser(Long contactUserId, Map<Long, Optional<User>> contactLookupCache) {
        if (contactLookupCache != null && contactLookupCache.containsKey(contactUserId)) {
            return contactLookupCache.get(contactUserId).orElse(null);
        }

        Optional<User> referencedUser = this.userRepository.findByAccountIdAndDeletedAtIsNull(contactUserId);
        if (contactLookupCache != null) {
            contactLookupCache.put(contactUserId, referencedUser);
        }

        return referencedUser.orElse(null);
    }

    private Long extractContactCardUserId(Message message) {
        if (message == null || message.getMessageType() != MessageType.CONTACT_CARD) {
            return null;
        }

        return parseContactCardUserId(message.getContent());
    }

    private Long parseContactCardUserId(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        try {
            long parsed = Long.parseLong(content.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private MessageResponse.ReplyContext buildReplyContext(
            Message message,
            Map<String, Message> localMessages,
            Map<String, Optional<Message>> parentLookupCache
    ) {
        String replyToMessageId = normalizeReplyToMessageId(message.getReplyTo());
        if (replyToMessageId == null) {
            return null;
        }

        String chatRoomId = message.getChatRoomId();
        if (chatRoomId == null || chatRoomId.isBlank()) {
            return MessageResponse.ReplyContext.builder()
                    .originalMessageId(replyToMessageId)
                    .originalContentPreview(UNAVAILABLE_MESSAGE_PREVIEW)
                    .originalMessageUnavailable(true)
                    .originalMessageHidden(false)
                    .build();
        }

        Message originalMessage = resolveOriginalMessage(
                chatRoomId,
                replyToMessageId,
                localMessages,
                parentLookupCache
        );

        if (originalMessage == null) {
            return MessageResponse.ReplyContext.builder()
                    .originalMessageId(replyToMessageId)
                    .originalContentPreview(UNAVAILABLE_MESSAGE_PREVIEW)
                    .originalMessageUnavailable(true)
                    .originalMessageHidden(false)
                    .build();
        }

        boolean originalMessageHidden = Boolean.TRUE.equals(originalMessage.getIsHidden());

        return MessageResponse.ReplyContext.builder()
                .originalMessageId(
                        originalMessage.getMessageId() != null
                                ? originalMessage.getMessageId()
                                : replyToMessageId
                )
                .originalSender(originalMessage.getSender())
                .originalMessageType(originalMessage.getMessageType())
                .originalContentPreview(
                        originalMessageHidden
                                ? REVOKED_MESSAGE_PREVIEW
                                : buildOriginalMessagePreview(originalMessage)
                )
                .originalMessageUnavailable(false)
                .originalMessageHidden(originalMessageHidden)
                .build();
    }

    private Message resolveOriginalMessage(
            String chatRoomId,
            String replyToMessageId,
            Map<String, Message> localMessages,
            Map<String, Optional<Message>> parentLookupCache
    ) {
        if (localMessages != null && localMessages.containsKey(replyToMessageId)) {
            return localMessages.get(replyToMessageId);
        }

        Optional<Message> parentMessage = findReplyTarget(chatRoomId, replyToMessageId, parentLookupCache);
        return parentMessage.orElse(null);
    }

    private Optional<Message> findReplyTarget(
            String chatRoomId,
            String replyToMessageId,
            Map<String, Optional<Message>> parentLookupCache
    ) {
        if (parentLookupCache != null && parentLookupCache.containsKey(replyToMessageId)) {
            return parentLookupCache.get(replyToMessageId);
        }

        Optional<Message> parentMessage = this.messageRepository
                .findByChatRoomIdAndMessageId(chatRoomId, replyToMessageId);

        if (parentLookupCache != null) {
            parentLookupCache.put(replyToMessageId, parentMessage);
        }

        return parentMessage;
    }

    private String buildOriginalMessagePreview(Message originalMessage) {
        if (originalMessage == null) {
            return UNAVAILABLE_MESSAGE_PREVIEW;
        }

        if (originalMessage.getMessageType() == MessageType.TEXT) {
            return truncateReplyPreview(originalMessage.getContent());
        }

        if (originalMessage.getMessageType() == MessageType.MEDIA) {
            return buildMediaPreview(originalMessage.getMediaItems());
        }

        if (originalMessage.getMessageType() == null) {
            return UNAVAILABLE_MESSAGE_PREVIEW;
        }

        return originalMessage.getMessageType().name().toLowerCase(Locale.ROOT);
    }

    private String buildMediaPreview(List<MediaItem> mediaItems) {
        if (mediaItems == null || mediaItems.isEmpty()) {
            return MessageType.MEDIA.name().toLowerCase(Locale.ROOT);
        }

        MediaItem first = mediaItems.get(0);
        if (first == null || first.getMediaType() == null) {
            return MessageType.MEDIA.name().toLowerCase(Locale.ROOT);
        }

        return first.getMediaType().name().toLowerCase(Locale.ROOT);
    }

    private String truncateReplyPreview(String content) {
        if (content == null || content.isBlank()) {
            return "text";
        }

        String normalized = content.trim();
        if (normalized.length() <= MAX_REPLY_PREVIEW_LENGTH) {
            return normalized;
        }

        return normalized.substring(0, MAX_REPLY_PREVIEW_LENGTH) + "...";
    }

    private String normalizeReplyToMessageId(String replyToMessageId) {
        if (replyToMessageId == null) {
            return null;
        }

        String normalized = replyToMessageId.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        return normalized;
    }

    private void validateReplyTarget(String chatRoomId, String replyToMessageId) throws InvalidException {
        if (replyToMessageId == null) {
            return;
        }

        boolean isValidReplyTarget = this.messageRepository
                .findByChatRoomIdAndMessageId(chatRoomId, replyToMessageId)
                .isPresent();

        if (!isValidReplyTarget) {
            throw new InvalidException("replyToMessageId is invalid or not in this conversation");
        }
    }

    private MessageType determineMessageType(String mimeType) {
        MediaType mediaType = determineMediaType(mimeType);
        if (mediaType != null) {
            return MessageType.MEDIA;
        }

        return MessageType.FILE;
    }

    private MessageType determineLegacyMediaMessageType(String mimeType) {
        MediaType mediaType = determineMediaType(mimeType);
        if (mediaType == null) {
            return MessageType.FILE;
        }

        return switch (mediaType) {
            case IMAGE -> MessageType.IMAGE;
            case VIDEO -> MessageType.VIDEO;
            case AUDIO -> MessageType.AUDIO;
        };
    }

    private MediaType determineMediaType(String mimeType) {
        if (mimeType == null) {
            return null;
        }

        String normalized = mimeType.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("image/")) {
            return MediaType.IMAGE;
        }
        if (normalized.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        if (normalized.startsWith("audio/")) {
            return MediaType.AUDIO;
        }

        return null;
    }

    private String normalizeMessageContent(String content) {
        if (content == null) {
            return null;
        }

        String normalized = content.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void validateFile(MultipartFile file) throws InvalidException {
        if (file == null || file.isEmpty()) {
            throw new InvalidException("File cannot be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidException(
                    String.format("File '%s' exceeds maximum size of 20MB",
                            file.getOriginalFilename()));
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.contains("..") || filename.contains("/")) {
            throw new InvalidException("Invalid filename");
        }
    }

    private void validateMediaBatchConstraints(List<MultipartFile> mediaFiles) throws InvalidException {
        if (mediaFiles == null || mediaFiles.isEmpty()) {
            return;
        }

        if (mediaFiles.size() > MAX_MEDIA_ITEMS_PER_MESSAGE) {
            throw new InvalidException("Cannot upload more than " + MAX_MEDIA_ITEMS_PER_MESSAGE + " media items per message");
        }

        long totalSize = 0L;
        for (MultipartFile mediaFile : mediaFiles) {
            if (mediaFile == null) {
                continue;
            }

            totalSize += mediaFile.getSize();
            if (totalSize > MAX_MEDIA_BATCH_SIZE) {
                throw new InvalidException("Total media payload exceeds maximum size of 100MB");
            }
        }
    }

    private String getFolderByMessageType(MessageType type) {
        return switch (type) {
            case IMAGE -> "images";
            case VIDEO -> "videos";
            case AUDIO -> "audios";
            default -> "files";
        };
    }

    private String getFolderByMediaType(MediaType mediaType) {
        return switch (mediaType) {
            case IMAGE -> "images";
            case VIDEO -> "videos";
            case AUDIO -> "audios";
        };
    }

    private Message createMediaMessage(
            String chatRoomId,
            List<MediaItem> mediaItems,
            String content,
            String replyToMessageId,
            Account currentAccount
    ) {
        String messageId = generateMessageId();
        Instant now = Instant.now();
        long timestamp = now.toEpochMilli();

        String messageSk = Message.buildMessageSk(timestamp, messageId);

        return Message.builder()
                .messageSk(messageSk)
                .chatRoomId(chatRoomId)
                .messageId(messageId)
                .sender(buildSenderInfo(currentAccount))
                .content(content)
                .mediaItems(cloneMediaItems(mediaItems))
                .messageType(MessageType.MEDIA)
                .replyTo(replyToMessageId)
                .isHidden(false)
                .isForwarded(false)
                .originalMessageId(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Message createFileMessage(
            String chatRoomId,
            String fileUrl,
            MessageType messageType,
            String replyToMessageId,
            Account currentAccount
    ) {
        String messageId = generateMessageId();
        Instant now = Instant.now();
        long timestamp = now.toEpochMilli();

        String messageSk = Message.buildMessageSk(timestamp, messageId);

        // Build sender information
        SenderInfo senderInfo = buildSenderInfo(currentAccount);

        return Message.builder()
                .messageSk(messageSk)
                .chatRoomId(chatRoomId)
                .messageId(messageId)
                .sender(senderInfo)  // NEW: Use embedded sender
                .content(fileUrl)
                .mediaItems(null)
                .messageType(messageType)
                .replyTo(replyToMessageId)
                .isHidden(false)
                .isForwarded(false)
                .originalMessageId(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Message createContactCardMessage(String chatRoomId, Long referencedUserId, Account currentAccount) {
        String messageId = generateMessageId();
        Instant now = Instant.now();
        long timestamp = now.toEpochMilli();

        return Message.builder()
                .messageSk(Message.buildMessageSk(timestamp, messageId))
                .chatRoomId(chatRoomId)
                .messageId(messageId)
                .sender(buildSenderInfo(currentAccount))
                .content(String.valueOf(referencedUserId))
            .mediaItems(null)
                .messageType(MessageType.CONTACT_CARD)
                .replyTo(null)
                .isHidden(false)
                .isForwarded(false)
                .originalMessageId(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Message createForwardedMessage(Message sourceMessage, Long targetChatRoomId, Account currentAccount) {
        String forwardedMessageId = generateMessageId();
        Instant now = Instant.now();
        long timestamp = now.toEpochMilli();

        return Message.builder()
                .messageSk(Message.buildMessageSk(timestamp, forwardedMessageId))
                .chatRoomId(targetChatRoomId.toString())
                .messageId(forwardedMessageId)
                .sender(buildSenderInfo(currentAccount))
                .content(sourceMessage.getContent())
            .mediaItems(cloneMediaItems(sourceMessage.getMediaItems()))
                .messageType(sourceMessage.getMessageType())
                .replyTo(null)
                .isHidden(false)
                .isForwarded(true)
                .originalMessageId(sourceMessage.getMessageId())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private List<MediaItem> cloneMediaItems(List<MediaItem> sourceMediaItems) {
        if (sourceMediaItems == null || sourceMediaItems.isEmpty()) {
            return null;
        }

        List<MediaItem> clonedItems = new ArrayList<>();
        for (MediaItem mediaItem : sourceMediaItems) {
            if (mediaItem == null || mediaItem.getUrl() == null || mediaItem.getUrl().isBlank()) {
                continue;
            }

            clonedItems.add(MediaItem.builder()
                    .url(mediaItem.getUrl())
                    .mediaType(mediaItem.getMediaType())
                    .mimeType(mediaItem.getMimeType())
                    .sizeBytes(mediaItem.getSizeBytes())
                    .displayOrder(mediaItem.getDisplayOrder())
                    .build());
        }

        return clonedItems.isEmpty() ? null : clonedItems;
    }

    private List<Message> collectCascadeRecallMessages(Message rootMessage) {
        if (rootMessage == null) {
            return Collections.emptyList();
        }

        LinkedHashMap<String, Message> messagesById = new LinkedHashMap<>();
        addMessageIfAbsent(messagesById, rootMessage);

        List<Message> forwardedDescendants = this.messageRepository
                .findForwardedDescendantsByOriginalMessageId(rootMessage.getMessageId());

        for (Message forwardedDescendant : forwardedDescendants) {
            addMessageIfAbsent(messagesById, forwardedDescendant);
        }

        return new ArrayList<>(messagesById.values());
    }

    private void addMessageIfAbsent(Map<String, Message> messagesById, Message message) {
        if (message == null || message.getMessageId() == null || message.getMessageId().isBlank()) {
            return;
        }

        messagesById.putIfAbsent(message.getMessageId(), message);
    }

    private List<Message> applyRecallState(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        Instant recallTime = Instant.now();
        List<Message> recalledMessages = new ArrayList<>();

        for (Message message : messages) {
            if (message == null || Boolean.TRUE.equals(message.getIsHidden())) {
                continue;
            }

            message.setIsHidden(true);
            message.setContent(null);
            message.setMediaItems(null);
            message.setUpdatedAt(recallTime);

            Message savedMessage = this.messageRepository.saveMessage(message);
            recalledMessages.add(savedMessage);
        }

        return recalledMessages;
    }

    private List<CascadeDeleteTarget> collectCascadeDeleteTargets(Message rootMessage) {
        if (rootMessage == null) {
            return Collections.emptyList();
        }

        LinkedHashMap<String, CascadeDeleteTarget> targetsByMessageId = new LinkedHashMap<>();

        List<Message> forwardedDescendants = this.messageRepository
                .findForwardedDescendantsByOriginalMessageId(rootMessage.getMessageId());

        List<Message> descendantsInDeleteOrder = new ArrayList<>(forwardedDescendants);
        Collections.reverse(descendantsInDeleteOrder);

        for (Message descendant : descendantsInDeleteOrder) {
            addCascadeDeleteTargetIfAbsent(targetsByMessageId, descendant, DELETE_TYPE_FORWARDED);
        }

        addCascadeDeleteTargetIfAbsent(targetsByMessageId, rootMessage, DELETE_TYPE_ORIGINAL);

        return new ArrayList<>(targetsByMessageId.values());
    }

    private void addCascadeDeleteTargetIfAbsent(
            Map<String, CascadeDeleteTarget> targetsByMessageId,
            Message message,
            String deleteType
    ) {
        if (message == null || message.getMessageId() == null || message.getMessageId().isBlank()) {
            return;
        }

        targetsByMessageId.putIfAbsent(message.getMessageId(), new CascadeDeleteTarget(message, deleteType));
    }

    private void deleteSingleMessagePermanently(Message message) throws InvalidException {
        if (message == null) {
            throw new InvalidException("Message is invalid");
        }

        String targetChatRoomId = message.getChatRoomId();
        String targetMessageId = message.getMessageId();
        String targetMessageSk = message.getMessageSk();

        if (targetChatRoomId == null || targetChatRoomId.isBlank()
                || targetMessageId == null || targetMessageId.isBlank()
                || targetMessageSk == null || targetMessageSk.isBlank()) {
            throw new InvalidException("Message data is invalid for permanent delete");
        }

        cleanupMessageBinaryContent(message);

        try {
            this.messageRepository.deletePinnedMessage(targetChatRoomId, targetMessageId);
            this.messageRepository.deleteMessage(targetChatRoomId, targetMessageSk);
        } catch (RuntimeException e) {
            log.error("Failed to permanently delete message item: messageId={}, chatRoomId={}",
                    targetMessageId,
                    targetChatRoomId,
                    e);
            throw new InvalidException("Failed to permanently delete message");
        }
    }

    private MessageDeletedEventResponse buildMessageDeletedEvent(
            Message message,
            String deleteType,
            Long deletedByAccountId,
            Instant deletedAt
    ) {
        return MessageDeletedEventResponse.builder()
                .eventType(MESSAGE_DELETED_EVENT)
                .chatRoomId(message.getChatRoomId())
                .messageId(message.getMessageId())
                .deleteType(deleteType)
                .deletedByAccountId(deletedByAccountId)
                .deletedAt(deletedAt)
                .build();
    }

    private record CascadeDeleteTarget(Message message, String deleteType) {
    }

    private String generateMessageId() {
        return "msg_" + UUID.randomUUID().toString().replace("-", "");
    }

    private void validateNoBlockedDirectInteraction(ChatRoom chatRoom, Long currentAccountId) throws InvalidException {
        if (chatRoom.getType() != ChatRoomType.DIRECT) {
            return;
        }

        Long peerAccountId = this.chatMemberRepository.findByRoomRoomIdAndDeletedAtIsNull(chatRoom.getRoomId())
                .stream()
                .map(ChatMember::getAccount)
                .filter(Objects::nonNull)
                .map(Account::getAccountId)
                .filter(accountId -> !Objects.equals(accountId, currentAccountId))
                .findFirst()
                .orElseThrow(() -> new InvalidException("Direct chat room has invalid members"));

        UserRelationshipRepository.PairIds pair = UserRelationshipRepository.PairIds.of(currentAccountId, peerAccountId);
        this.userRelationshipRepository.lockPairForTransactionByCanonicalIds(pair.pairLowId(), pair.pairHighId());

        boolean blocked = this.userRelationshipRepository
                .existsByPairLowUser_AccountIdAndPairHighUser_AccountIdAndRelationshipStateAndDeletedAtIsNull(
                        pair.pairLowId(),
                        pair.pairHighId(),
                        RelationshipState.BLOCKED
                );

        if (blocked) {
            throw new BlockedInteractionException("Cannot send message because this pair is blocked");
        }
    }

    private List<Long> normalizeContactCardUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<Long> deduplicated = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId != null && userId > 0) {
                deduplicated.add(userId);
            }
        }

        return new ArrayList<>(deduplicated);
    }

    private boolean isFriendWithCurrentUser(Long currentAccountId, Long targetUserId) {
        if (currentAccountId == null || targetUserId == null || Objects.equals(currentAccountId, targetUserId)) {
            return false;
        }

        UserRelationshipRepository.PairIds pair = UserRelationshipRepository.PairIds.of(currentAccountId, targetUserId);
        return this.userRelationshipRepository
                .existsByPairLowUser_AccountIdAndPairHighUser_AccountIdAndRelationshipStateAndDeletedAtIsNull(
                        pair.pairLowId(),
                        pair.pairHighId(),
                        RelationshipState.FRIEND
                );
    }

    private void sendMessageToUsers(String chatRoomId, Message message) {
        if (chatRoomId == null || chatRoomId.isBlank() || message == null) {
            log.warn("Skip realtime emit because payload is invalid: chatRoomId={}, messageNull={}",
                    chatRoomId,
                    message == null);
            return;
        }

        log.debug("Sending realtime message to chatRoom: {}", chatRoomId);
        MessageResponse payload = toMessageResponse(message);
        messagingTemplate.convertAndSend("/topic/chatrooms/" + chatRoomId, payload);
    }

    private void sendMessageDeletedEvent(Long chatRoomId, MessageDeletedEventResponse event) {
        if (chatRoomId == null) {
            log.warn("Skip delete event because chatRoomId is null for messageId={}",
                    event != null ? event.getMessageId() : null);
            return;
        }

        sendMessageDeletedEvent(chatRoomId.toString(), event);
    }

    private void sendMessageDeletedEvent(String chatRoomId, MessageDeletedEventResponse event) {
        if (chatRoomId == null || chatRoomId.isBlank() || event == null) {
            log.warn("Skip delete event because payload is invalid: chatRoomId={}, eventNull={}",
                    chatRoomId,
                    event == null);
            return;
        }

        log.debug("Sending delete event to chatRoom: {}, messageId={}, deleteType={}",
                chatRoomId,
                event.getMessageId(),
                event.getDeleteType());

        try {
            messagingTemplate.convertAndSend("/topic/chatrooms/" + chatRoomId, event);
        } catch (RuntimeException e) {
            log.error("Failed to send delete event: chatRoomId={}, messageId={}, deleteType={}",
                    chatRoomId,
                    event.getMessageId(),
                    event.getDeleteType(),
                    e);
        }
    }

    private boolean isSuperAdmin(Account account) {
        return account.getRole() != null
                && account.getRole().getName() != null
                && Role.ADMIN.getValue().equalsIgnoreCase(account.getRole().getName());
    }

    private void cleanupMessageBinaryContent(Message message) throws InvalidException {
        if (message == null) {
            return;
        }

        MessageType messageType = message.getMessageType();
        boolean hasMediaItems = message.getMediaItems() != null && !message.getMediaItems().isEmpty();

        if (!hasMediaItems && !isBinaryMessageType(messageType)) {
            return;
        }

        LinkedHashSet<String> storageKeys = new LinkedHashSet<>();
        collectStorageKeysFromMediaItems(message.getMediaItems(), storageKeys);

        if (requiresLegacyContentCleanup(messageType)) {
            addStorageKeyFromContent(message.getContent(), storageKeys);
        }

        for (String key : storageKeys) {
            deleteStorageObjectByKey(message.getMessageId(), key);
        }
    }

    private void collectStorageKeysFromMediaItems(List<MediaItem> mediaItems, Set<String> storageKeys)
            throws InvalidException {
        if (mediaItems == null || mediaItems.isEmpty()) {
            return;
        }

        for (MediaItem mediaItem : mediaItems) {
            if (mediaItem == null || mediaItem.getUrl() == null || mediaItem.getUrl().isBlank()) {
                continue;
            }

            String key = extractStorageKey(mediaItem.getUrl());
            if (key == null || key.isBlank()) {
                throw new InvalidException("Cannot determine storage key for deleting media item content");
            }

            storageKeys.add(key);
        }
    }

    private boolean requiresLegacyContentCleanup(MessageType messageType) {
        return messageType == MessageType.IMAGE
                || messageType == MessageType.VIDEO
                || messageType == MessageType.AUDIO
                || messageType == MessageType.FILE;
    }

    private void addStorageKeyFromContent(String content, Set<String> storageKeys) throws InvalidException {
        if (content == null || content.isBlank()) {
            return;
        }

        String key = extractStorageKey(content);
        if (key == null || key.isBlank()) {
            throw new InvalidException("Cannot determine storage key for deleting message content");
        }

        storageKeys.add(key);
    }

    private void deleteStorageObjectByKey(String messageId, String key) throws InvalidException {
        try {
            this.storageService.handleDeleteFile(key);
        } catch (Exception e) {
            log.error("Failed to delete storage object for messageId={}", messageId, e);
            throw new InvalidException("Failed to delete message file from storage");
        }
    }

    private boolean isBinaryMessageType(MessageType messageType) {
        return messageType == MessageType.MEDIA
                || messageType == MessageType.IMAGE
                || messageType == MessageType.VIDEO
                || messageType == MessageType.AUDIO
                || messageType == MessageType.FILE;
    }

    private boolean isUserInChatRoom(Long chatRoomId, Long accountId) {
        List<ChatMember> members = this.chatMemberRepository.findByRoomRoomIdAndDeletedAtIsNull(chatRoomId);
        return members.stream()
                .anyMatch(member -> member.getAccount() != null
                        && Objects.equals(member.getAccount().getAccountId(), accountId));
    }

    private List<Long> normalizeTargetChatRoomIds(ForwardMessageRequest request) throws InvalidException {
        if (request == null || request.getTargetChatRoomIds() == null || request.getTargetChatRoomIds().isEmpty()) {
            throw new InvalidException("At least one target chat room ID is required");
        }

        LinkedHashSet<Long> deduplicated = new LinkedHashSet<>();
        for (Long chatRoomId : request.getTargetChatRoomIds()) {
            if (chatRoomId != null) {
                deduplicated.add(chatRoomId);
            }
        }

        if (deduplicated.isEmpty()) {
            throw new InvalidException("At least one valid target chat room ID is required");
        }

        if (deduplicated.size() > MAX_FORWARD_TARGETS) {
            throw new InvalidException("Cannot forward to more than " + MAX_FORWARD_TARGETS + " chat rooms at once");
        }

        return new ArrayList<>(deduplicated);
    }

    private void validateForwardableSourceMessage(Message sourceMessage) throws InvalidException {
        if (Boolean.TRUE.equals(sourceMessage.getIsHidden())) {
            throw new InvalidException("Cannot forward a revoked message");
        }

        if (sourceMessage.getMessageType() == MessageType.SYSTEM) {
            throw new InvalidException("System messages cannot be forwarded");
        }

        if (!hasForwardablePayload(sourceMessage)) {
            throw new InvalidException("Cannot forward an empty message");
        }
    }

    private boolean hasForwardablePayload(Message sourceMessage) {
        if (sourceMessage == null) {
            return false;
        }

        if (sourceMessage.getContent() != null && !sourceMessage.getContent().isBlank()) {
            return true;
        }

        List<MediaItem> mediaItems = sourceMessage.getMediaItems();
        if (mediaItems == null || mediaItems.isEmpty()) {
            return false;
        }

        for (MediaItem mediaItem : mediaItems) {
            if (mediaItem != null && mediaItem.getUrl() != null && !mediaItem.getUrl().isBlank()) {
                return true;
            }
        }

        return false;
    }

    private ForwardMessageFailureResponse buildForwardFailure(Long targetChatRoomId, String code, String message) {
        return ForwardMessageFailureResponse.builder()
                .targetChatRoomId(targetChatRoomId)
                .code(code)
                .message(message)
                .build();
    }

    private String extractStorageKey(String content) {
        String trimmed = content.trim();
        if (!(trimmed.startsWith("http://") || trimmed.startsWith("https://"))) {
            return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        }

        try {
            URI uri = new URI(trimmed);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private Long extractSenderAccountId(Message message) {
        if (message.getSender() != null && message.getSender().getAccountId() != null) {
            return message.getSender().getAccountId();
        }

        String senderId = message.getSenderId();
        if (senderId == null || senderId.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(senderId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private SenderInfo buildSenderInfo(Account account) {
        String fullName = account instanceof Company ? ((Company) account).getName()
                : ((User) account).getFullName();

        String avatar = account instanceof Company ? ((Company) account).getLogo()
                : account.getAvatar();

        return SenderInfo.builder()
                .accountId(account.getAccountId())
                .fullName(fullName)
                .username(account.getUsername())
                .email(account.getEmail())
                .avatar(avatar)
                .build();
    }
}