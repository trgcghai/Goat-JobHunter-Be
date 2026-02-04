package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.dto.response.StorageResponse;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.entity.embeddable.SenderInfo;
import iuh.fit.goat.enumeration.MessageType;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.service.MessageService;
import iuh.fit.goat.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final DateTimeFormatter BUCKET_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_MESSAGES_PER_BUCKET = 100;
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final StorageService storageService;
    private final SimpMessagingTemplate messagingTemplate;

    // ========== PUBLIC API METHODS ==========

    /**
     * Get last message with aggressive multi-bucket fallback
     * Never returns null if messages exist in any bucket
     */
    @Override
    public Message getLastMessageByChatRoom(Long chatRoomId) throws InvalidException {
        if (chatRoomId == null) {
            throw new InvalidException("Chat room ID cannot be null");
        }

        chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new InvalidException("Chat Room not found"));

        Optional<Message> lastMessage = messageRepository
                .findLastMessageByConversation(chatRoomId.toString());

        if (lastMessage.isEmpty()) {
            log.warn("No messages found in any bucket for chatRoom: {}", chatRoomId);
        }

        return lastMessage.orElse(null);
    }

    /**
     * Get messages with smart multi-bucket merging
     * Strategy:
     * 1. Use new findMessagesAcrossBuckets() method
     * 2. Automatically fetches from multiple buckets if needed
     * 3. Returns sorted and paginated results
     */
    @Override
    public List<Message> getMessagesByChatRoom(Long chatRoomId, Pageable pageable) {
        if (chatRoomId == null) {
            throw new IllegalArgumentException("Chat room ID cannot be null");
        }

        int requestedSize = pageable.getPageSize();

        log.info("Fetching up to {} messages for chatRoom: {}", requestedSize, chatRoomId);

        // NEW: Use multi-bucket query method
        List<Message> messages = messageRepository.findMessagesAcrossBuckets(
                chatRoomId.toString(),
                requestedSize,
                false // Don't include hidden messages
        );

        log.info("Retrieved {} messages for chatRoom: {}", messages.size(), chatRoomId);

        return messages;
    }

    /**
     * Send message with SMART bucket selection
     * Rules:
     * 1. Reuse today's bucket if < 100 messages
     * 2. Reuse yesterday's bucket if today hasn't started AND < 100 messages
     * 3. Create overflow bucket if today's bucket is full
     */
    @Override
    public Message sendMessage(Long chatRoomId, MessageCreateRequest request, User currentUser)
            throws InvalidException {

        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new InvalidException("Chat Room not found"));

        String bucketKey = getOrCreateSmartBucket(chatRoomId.toString());
        String messageId = generateMessageId();
        Instant now = Instant.now();
        long timestamp = now.toEpochMilli();

        String chatRoomBucket = Message.buildChatRoomBucket(chatRoomId.toString(), bucketKey);
        String messageSk = Message.buildMessageSk(timestamp, messageId);

        // Build sender information
        SenderInfo senderInfo = buildSenderInfo(currentUser);

        Message message = Message.builder()
                .chatRoomBucket(chatRoomBucket)
                .messageSk(messageSk)
                .chatRoomId(chatRoomId.toString())
                .bucket(bucketKey)
                .messageId(messageId)
                .sender(senderInfo)  // NEW: Use embedded sender
                .content(request.getContent())
                .messageType(MessageType.TEXT)
                .isHidden(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        log.info("Saving message - PK: {}, SK: {}", chatRoomBucket, messageSk);

        Message savedMessage = messageRepository.saveMessage(message);

        log.info("Message created: messageId={}, bucket={}, chatRoomId={}",
                messageId, bucketKey, chatRoomId);

        sendMessageToUsers(chatRoomId, savedMessage);

        return savedMessage;
    }

    /**
     * Send messages with files (batch operation)
     */
    @Override
    public List<Message> sendMessagesWithFiles(
            Long chatRoomId,
            MessageCreateRequest request,
            List<MultipartFile> files,
            User currentUser) throws InvalidException {

        chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new InvalidException("Chat Room not found"));

        // NEW: Smart bucket selection (reused for all messages in this batch)
        String bucketKey = getOrCreateSmartBucket(chatRoomId.toString());

        List<Message> createdMessages = new ArrayList<>();

        try {
            // Process files first
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    validateFile(file);

                    String mimeType = file.getContentType();
                    MessageType messageType = determineMessageType(mimeType);

                    String folder = getFolderByMessageType(messageType);
                    StorageResponse storageResponse = storageService.handleUploadFile(file, folder);
                    String fileUrl = storageResponse.getUrl();

                    Message fileMessage = createFileMessage(
                            chatRoomId.toString(),
                            bucketKey, // Use same bucket
                            fileUrl,
                            messageType,
                            currentUser
                    );

                    Message savedMessage = messageRepository.saveMessage(fileMessage);
                    createdMessages.add(savedMessage);
                    sendMessageToUsers(chatRoomId, savedMessage);

                    log.info("File message created: messageId={}, type={}, bucket={}",
                            savedMessage.getMessageId(), messageType, bucketKey);
                }
            }

            // Process text content
            if (request != null && request.getContent() != null && !request.getContent().isBlank()) {
                MessageCreateRequest textRequest = new MessageCreateRequest(request.getContent());
                Message textMessage = sendMessage(chatRoomId, textRequest, currentUser);
                createdMessages.add(textMessage);
            }

            if (createdMessages.isEmpty()) {
                throw new InvalidException("At least one file or text content is required");
            }

            log.info("Batch message creation completed: {} messages created in bucket {}",
                    createdMessages.size(), bucketKey);

            return createdMessages;

        } catch (Exception e) {
            log.error("Error creating messages", e);
            throw new InvalidException("Failed to create messages: " + e.getMessage());
        }
    }

    @Override
    public void sendMessageToUsers(Long chatRoomId, Message message) {
        log.debug("Sending realtime message to chatRoom: {}", chatRoomId);
        messagingTemplate.convertAndSend("/topic/chatrooms/" + chatRoomId, message);
    }

    @Override
    public List<Message> getMediaMessagesByChatRoom(Long chatRoomId, Pageable pageable) throws InvalidException {
        if (chatRoomId == null) {
            throw new InvalidException("Chat room ID cannot be null");
        }

        chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new InvalidException("Chat Room not found"));

        int requestedSize = pageable.getPageSize();

        log.info("Fetching up to {} media messages for chatRoom: {}", requestedSize, chatRoomId);

        List<Message> allMessages = messageRepository.findMessagesAcrossBuckets(
                chatRoomId.toString(),
                requestedSize * 3, // Fetch more to filter
                false
        );

        List<Message> mediaMessages = allMessages.stream()
                .filter(msg -> isMediaType(msg.getMessageType()))
                .limit(requestedSize)
                .toList();

        log.info("Retrieved {} media messages for chatRoom: {}", mediaMessages.size(), chatRoomId);

        return mediaMessages;
    }

    @Override
    public List<Message> getFileMessagesByChatRoom(Long chatRoomId, Pageable pageable) throws InvalidException {
        if (chatRoomId == null) {
            throw new InvalidException("Chat room ID cannot be null");
        }

        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new InvalidException("Chat Room not found"));

        int requestedSize = pageable.getPageSize();

        log.info("Fetching up to {} file messages for chatRoom: {}", requestedSize, chatRoomId);

        List<Message> allMessages = messageRepository.findMessagesAcrossBuckets(
                chatRoomId.toString(),
                requestedSize * 3, // Fetch more to filter
                false
        );

        List<Message> fileMessages = allMessages.stream()
                .filter(msg -> msg.getMessageType() == MessageType.FILE)
                .limit(requestedSize)
                .toList();

        log.info("Retrieved {} file messages for chatRoom: {}", fileMessages.size(), chatRoomId);

        return fileMessages;
    }

    private boolean isMediaType(MessageType type) {
        return type == MessageType.IMAGE || type == MessageType.VIDEO || type == MessageType.AUDIO;
    }

    // ========== SMART BUCKET LOGIC ==========

    /**
     * Smart bucket selection with rollover logic
     * Rules:
     * 1. Check today's bucket → if < 100 messages, reuse it
     * 2. If today's bucket is full → create overflow bucket with timestamp
     * 3. Check yesterday's bucket → if < 100 messages AND no messages today, reuse it
     * 4. Default → create today's bucket
     * <p>
     * Bucket naming:
     * - Normal: "20250521"
     * - Overflow: "20250521_1716300000123"
     */
    private String getOrCreateSmartBucket(String chatRoomId) {
        LocalDate today = LocalDate.now();
        String todayBucket = formatBucket(today);

        // Step 1: Check today's bucket
        long todayMessageCount = messageRepository.countMessagesInBucket(chatRoomId, todayBucket);

        if (todayMessageCount > 0 && todayMessageCount < MAX_MESSAGES_PER_BUCKET) {
            log.info("Reusing today's bucket: {} ({}/100 messages)", todayBucket, todayMessageCount);
            return todayBucket;
        }

        if (todayMessageCount >= MAX_MESSAGES_PER_BUCKET) {
            // Create overflow bucket
            String overflowBucket = todayBucket + "_" + System.currentTimeMillis();
            log.info("Today's bucket is full, creating overflow bucket: {}", overflowBucket);
            return overflowBucket;
        }

        // Step 2: Check yesterday's bucket (if today has no messages)
        LocalDate yesterday = today.minusDays(1);
        String yesterdayBucket = formatBucket(yesterday);
        long yesterdayMessageCount = messageRepository.countMessagesInBucket(chatRoomId, yesterdayBucket);

        if (yesterdayMessageCount > 0 && yesterdayMessageCount < MAX_MESSAGES_PER_BUCKET) {
            log.info("Reusing yesterday's bucket: {} ({}/100 messages)",
                    yesterdayBucket, yesterdayMessageCount);
            return yesterdayBucket;
        }

        // Step 3: Default - use today's bucket (new or existing)
        log.info("Using today's bucket: {} (new or empty)", todayBucket);
        return todayBucket;
    }

    // ========== HELPER METHODS ==========

    private MessageType determineMessageType(String mimeType) {
        if (mimeType == null) return MessageType.FILE;
        String type = mimeType.toLowerCase();
        if (type.startsWith("image/")) return MessageType.IMAGE;
        if (type.startsWith("video/")) return MessageType.VIDEO;
        if (type.startsWith("audio/")) return MessageType.AUDIO;
        return MessageType.FILE;
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

    private String getFolderByMessageType(MessageType type) {
        switch (type) {
            case IMAGE:
                return "images";
            case VIDEO:
                return "videos";
            case AUDIO:
                return "audios";
            default:
                return "files";
        }
    }

    private Message createFileMessage(
            String chatRoomId,
            String bucketKey,
            String fileUrl,
            MessageType messageType,
            User currentUser) {

        String messageId = generateMessageId();
        Instant now = Instant.now();
        long timestamp = now.toEpochMilli();

        String chatRoomBucket = Message.buildChatRoomBucket(chatRoomId, bucketKey);
        String messageSk = Message.buildMessageSk(timestamp, messageId);

        // Build sender information
        SenderInfo senderInfo = buildSenderInfo(currentUser);

        return Message.builder()
                .chatRoomBucket(chatRoomBucket)
                .messageSk(messageSk)
                .chatRoomId(chatRoomId)
                .bucket(bucketKey)
                .messageId(messageId)
                .sender(senderInfo)  // NEW: Use embedded sender
                .content(fileUrl)
                .messageType(messageType)
                .isHidden(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private String generateMessageId() {
        return "msg_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String formatBucket(LocalDate date) {
        return date.format(BUCKET_FORMATTER);
    }

    private SenderInfo buildSenderInfo(User user) {
        return SenderInfo.builder()
                .accountId(user.getAccountId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .build();
    }
}