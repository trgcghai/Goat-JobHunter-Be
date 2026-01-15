package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.MessageType;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final DateTimeFormatter BUCKET_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;

    // ========== Message Operations ==========

    @Override
    public Message getLastMessageByChatRoom(Long chatRoomId) throws InvalidException {
        // Validate parameters
        if (chatRoomId == null) {
            throw new InvalidException("Chat room ID cannot be null");
        }

        // Validate if chat room is exist with received ID
        ChatRoom chatRoom = this.chatRoomRepository.findById(chatRoomId).orElse(null);
        if (chatRoom == null) {
            throw new InvalidException("Chat Room not found");
        }

        return messageRepository.findLastMessageByConversation(chatRoomId.toString()).orElse(null);
    }

    @Override
    public List<Message> getMessagesByChatRoom(Long chatRoomId, Pageable pageable) {
        if (chatRoomId == null) {
            throw new IllegalArgumentException("Chat room ID cannot be null");
        }

        String conversationId = chatRoomId.toString();
        String currentBucket = getCurrentBucket();

        return messageRepository.findMessagesByBucket(
                conversationId, currentBucket, false);
    }

    @Override
    public Message sendMessage(Long chatRoomId, MessageCreateRequest request, User currentUser) throws InvalidException {

        // Validate and check chat room exist
        ChatRoom chatRoom = this.chatRoomRepository.findById(chatRoomId).orElse(null);

        if (chatRoom == null) {
            throw new InvalidException("Chat Room not found");
        }

        // Generate IDs and timestamps
        String messageId = generateMessageId();
        Instant now = Instant.now();
        long timestamp = now.toEpochMilli();
        String bucket = getBucketFromInstant(now);

        String chatRoomBucket = Message.buildChatRoomBucket(chatRoomId.toString(), bucket);
        String messageSk = Message.buildMessageSk(timestamp, messageId);

        // Build message entity
        Message message = Message.builder()
                .chatRoomBucket(chatRoomBucket)
                .messageSk(messageSk)
                .chatRoomId(chatRoomId.toString())
                .bucket(bucket)
                .messageId(messageId)
                .senderId(String.valueOf(currentUser.getAccountId()))
                .content(request.getContent())
                .messageType(String.valueOf(MessageType.TEXT))
                .isHidden(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        log.info("Saving message - PK: {}, SK: {}",
                message.getChatRoomBucket(), message.getMessageSk());

        // Save message
        Message savedMessage = messageRepository.saveMessage(message);

        log.info("Message created: messageId={}, conversationId={}, senderId={}",
                messageId, chatRoomId, currentUser.getAccountId());

        return savedMessage;
    }

//    @Override
//    public Message hideMessage(String messageId, String chatRoomId, String hiddenByAccountId) {
//        // Validation
//        if (messageId == null || messageId.isBlank()) {
//            throw new IllegalArgumentException("Message ID is required");
//        }
//        if (chatRoomId == null || chatRoomId.isBlank()) {
//            throw new IllegalArgumentException("Conversation ID is required");
//        }
//        if (hiddenByAccountId == null || hiddenByAccountId.isBlank()) {
//            throw new IllegalArgumentException("Hidden by account ID is required");
//        }
//
//        // Note: This is a simplified approach
//        // For production, you'd need a GSI on messageId for efficient lookup
//        log.warn("hideMessage operation requires scanning buckets - consider adding GSI on messageId");
//
//        // Try to find message in recent buckets
//        String currentBucket = getCurrentBucket();
//        Message message = null;
//
//        for (int i = 0; i < 7; i++) { // Search last 7 days
//            String conversationBucket = Message.buildChatRoomBucket(chatRoomId, currentBucket);
//            List<Message> messages = messageRepository.findMessagesByBucket(
//                    chatRoomId, currentBucket, 100, false);
//
//            message = messages.stream()
//                    .filter(m -> messageId.equals(m.getMessageId()))
//                    .findFirst()
//                    .orElse(null);
//
//            if (message != null) {
//                break;
//            }
//
//            currentBucket = getPreviousBucket(currentBucket);
//        }
//
//        if (message == null) {
//            throw new IllegalStateException("Message not found: " + messageId);
//        }
//
//        // Update message
//        message.setIsHidden(true);
//        message.setUpdatedAt(Instant.now());
//
//        Message updatedMessage = messageRepository.updateMessage(message);
//
//        log.info("Message hidden: messageId={}, conversationId={}, hiddenBy={}",
//                messageId, chatRoomId, hiddenByAccountId);
//
//        return updatedMessage;
//    }
//
//    // ========== PinnedMessage Operations ==========
//
//    @Override
//    public PinnedMessage pinMessage(Long chatRoomId, String messageId, String pinnedBy) {
//        // Validation
//        if (chatRoomId == null) {
//            throw new IllegalArgumentException("Chat room ID is required");
//        }
//        if (messageId == null || messageId.isBlank()) {
//            throw new IllegalArgumentException("Message ID is required");
//        }
//        if (pinnedBy == null || pinnedBy.isBlank()) {
//            throw new IllegalArgumentException("Pinned by user is required");
//        }
//
//        String conversationId = chatRoomId.toString();
//
//        // Check if already pinned
//        if (messageRepository.existsPinnedMessage(conversationId, messageId)) {
//            throw new IllegalStateException("Message is already pinned in this conversation");
//        }
//
//        // Create pinned message
//        Instant pinnedAt = Instant.now();
//        String pinnedSk = PinnedMessage.buildPinnedSk(pinnedAt.toEpochMilli(), messageId);
//        String messageBucket = getCurrentBucket();
//
//        PinnedMessage pinnedMessage = PinnedMessage.builder()
//                .chatRoomId(conversationId)
//                .pinnedSk(pinnedSk)
//                .messageId(messageId)
//                .messageBucket(messageBucket)
//                .pinnedBy(pinnedBy)
//                .pinnedAt(pinnedAt)
//                .build();
//
//        PinnedMessage saved = messageRepository.savePinnedMessage(pinnedMessage);
//
//        log.info("Message pinned: chatRoomId={}, messageId={}, pinnedBy={}",
//                chatRoomId, messageId, pinnedBy);
//
//        return saved;
//    }
//
//    @Override
//    public void unpinMessage(Long chatRoomId, String messageId) {
//        // Validation
//        if (chatRoomId == null) {
//            throw new IllegalArgumentException("Chat room ID is required");
//        }
//        if (messageId == null || messageId.isBlank()) {
//            throw new IllegalArgumentException("Message ID is required");
//        }
//
//        String conversationId = chatRoomId.toString();
//
//        // Find the pinned message
//        PinnedMessage pinnedMessage = messageRepository
//                .findPinnedMessageByConversationAndMessageId(conversationId, messageId)
//                .orElse(null);
//
//        if (pinnedMessage == null) {
//            log.debug("Message not pinned (idempotent): chatRoomId={}, messageId={}",
//                     chatRoomId, messageId);
//            return; // Idempotent - no error if not exists
//        }
//
//        // Delete the pinned message
//        messageRepository.deletePinnedMessage(
//                pinnedMessage.getChatRoomId(),
//                pinnedMessage.getPinnedSk()
//        );
//
//        log.info("Message unpinned: chatRoomId={}, messageId={}", chatRoomId, messageId);
//    }
//
//    @Override
//    public List<PinnedMessage> getPinnedMessages(Long chatRoomId) {
//        if (chatRoomId == null) {
//            throw new IllegalArgumentException("Chat room ID is required");
//        }
//
//        String conversationId = chatRoomId.toString();
//        return messageRepository.findAllPinnedMessagesByConversation(conversationId);
//    }
//
//    @Override
//    public boolean isMessagePinned(Long chatRoomId, String messageId) {
//        if (chatRoomId == null || messageId == null || messageId.isBlank()) {
//            return false;
//        }
//
//        String conversationId = chatRoomId.toString();
//        return messageRepository.existsPinnedMessage(conversationId, messageId);
//    }

    // ========== Helper Methods ==========
    private String generateMessageId() {
        return "msg_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String getCurrentBucket() {
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        return now.format(BUCKET_FORMATTER);
    }

    private String getBucketFromInstant(Instant instant) {
        LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        return date.format(BUCKET_FORMATTER);
    }
//
//    private String getPreviousBucket(String bucket) {
//        try {
//            LocalDate date = LocalDate.parse(bucket, BUCKET_FORMATTER);
//            LocalDate previousDate = date.minusDays(1);
//            return previousDate.format(BUCKET_FORMATTER);
//        } catch (Exception e) {
//            log.error("Error parsing bucket date: {}", bucket, e);
//            return bucket;
//        }
//    }
}