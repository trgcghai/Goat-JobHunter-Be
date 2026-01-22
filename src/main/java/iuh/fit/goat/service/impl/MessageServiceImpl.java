package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.dto.response.StorageResponse;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final DateTimeFormatter BUCKET_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;

    private final StorageService storageService;

    private final SimpMessagingTemplate messagingTemplate;

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
                .messageType(MessageType.TEXT)
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

        // Send realtime message to chat room members
        sendMessageToUsers(chatRoomId, savedMessage);

        return savedMessage;
    }

    /**
     * Send batch messages: files + optional text content
     * Mỗi file/content tạo 1 message riêng
     */
    @Override
    public List<Message> sendMessagesWithFiles(
            Long chatRoomId,
            MessageCreateRequest request,
            List<MultipartFile> files,
            User currentUser) throws InvalidException {

        // Validate chat room exists
        ChatRoom chatRoom = this.chatRoomRepository
                .findById(chatRoomId).orElse(null);

        if (chatRoom == null) {
            throw new InvalidException("Chat Room not found");
        }

        List<Message> createdMessages = new ArrayList<>();

        try {
            // 1. Process files first - mỗi file = 1 message
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    // Validate file
                    validateFile(file);

                    // Determine message type from MIME type
                    String mimeType = file.getContentType();
                    MessageType messageType = determineMessageType(mimeType);

                    // Upload file to S3
                    String folder = getFolderByMessageType(messageType);
                    StorageResponse storageResponse = storageService
                            .handleUploadFile(file, folder);

                    String fileUrl = storageResponse.getUrl();

                    // Create message with file URL
                    Message fileMessage = createFileMessage(
                            chatRoomId,
                            fileUrl,
                            messageType,
                            currentUser
                    );

                    // Save message
                    Message savedMessage = messageRepository
                            .saveMessage(fileMessage);

                    log.info("File message created: messageId={}, type={}, file={}",
                            savedMessage.getMessageId(),
                            messageType,
                            file.getOriginalFilename());

                    createdMessages.add(savedMessage);

                    // Send realtime message to chat room members
                    sendMessageToUsers(chatRoomId, savedMessage);
                }
            }

            // 2. Process text content - nếu có content thì tạo thêm 1 text message
            if (request != null && request.getContent() != null && !request.getContent().isBlank()) {

                MessageCreateRequest textRequest = new MessageCreateRequest(request.getContent());
                Message textMessage = sendMessage(chatRoomId, textRequest, currentUser);

                createdMessages.add(textMessage);

                log.info("Text message created: messageId={}", textMessage.getMessageId());
            }

            // Validate: phải có ít nhất 1 file hoặc content
            if (createdMessages.isEmpty()) {
                throw new InvalidException("At least one file or text content is required");
            }

            log.info("Batch message creation completed: {} messages created", createdMessages.size());

            return createdMessages;

        } catch (Exception e) {
            log.error("Error creating messages, rolling back uploaded files", e);
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void sendMessageToUsers(Long chatRoomId, Message message) {
        log.info("Sending realtime message to chat room {}: messageId={}",
                chatRoomId, message.getMessageId());

        // Send to specific chat room topic
        this.messagingTemplate.convertAndSend(
                "/topic/chatrooms/" + chatRoomId,
                message
        );

        log.info("Realtime message sent to chat room {} successfully", chatRoomId);
    }

    // ========== Helper Methods ==========

    /**
     * Xác định MessageType dựa trên MIME type của file
     */
    private MessageType determineMessageType(String mimeType) {
        if (mimeType == null) {
            return MessageType.FILE;
        }

        String type = mimeType.toLowerCase();
        if (type.startsWith("image/")) {
            return MessageType.IMAGE;
        } else if (type.startsWith("video/")) {
            return MessageType.VIDEO;
        } else if (type.startsWith("audio/")) {
            return MessageType.AUDIO;
        } else {
            return MessageType.FILE;
        }
    }

    /**
     * Validate file trước khi upload
     */
    private void validateFile(MultipartFile file) throws InvalidException {
        if (file == null || file.isEmpty()) {
            throw new InvalidException("File cannot be empty");
        }

        // Max 20MB per file
        long maxSize = 20 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new InvalidException(
                    String.format("File '%s' exceeds maximum size of 20MB",
                            file.getOriginalFilename()));
        }

        // Validate filename
        String filename = file.getOriginalFilename();
        if (filename == null || filename.contains("..") || filename.contains("/")) {
            throw new InvalidException("Invalid filename");
        }
    }

    /**
     * Xác định folder trong S3 dựa trên MessageType
     */
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

    /**
     * Tạo 1 message cho file đã upload
     */
    private Message createFileMessage(Long chatRoomId, String fileUrl, MessageType messageType, User currentUser) {

        String messageId = generateMessageId();
        Instant now = Instant.now();
        long timestamp = now.toEpochMilli();
        String bucket = getBucketFromInstant(now);

        String chatRoomBucket = Message.buildChatRoomBucket(
                chatRoomId.toString(), bucket);
        String messageSk = Message.buildMessageSk(timestamp, messageId);

        return Message.builder()
                .chatRoomBucket(chatRoomBucket)
                .messageSk(messageSk)
                .chatRoomId(chatRoomId.toString())
                .bucket(bucket)
                .messageId(messageId)
                .senderId(String.valueOf(currentUser.getAccountId()))
                .content(fileUrl) // URL của file
                .messageType(messageType)
                .isHidden(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

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