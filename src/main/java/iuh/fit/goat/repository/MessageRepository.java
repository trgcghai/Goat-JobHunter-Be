package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.PinnedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
@RequiredArgsConstructor
public class MessageRepository {

    private static final DateTimeFormatter BUCKET_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int BUCKET_SEARCH_LIMIT = 7; // Search up to 7 days back
    private static final int DEFAULT_LIMIT = 100; // Search 100 message once

    private final DynamoDbTable<Message> messageTable;
    private final DynamoDbTable<PinnedMessage> pinnedMessageTable;

    // ========== Message Operations ==========

    /**
     * Find the last (newest) message for a chat room
     * @param chatRoomId the Chat Room ID (mapped from chatRoomId)
     * @return Optional containing the last message
     */
    public Optional<Message> findLastMessageByConversation(String chatRoomId) {
        try {
            String currentBucket = getCurrentBucket();
            int daysSearched = 0;

            while (daysSearched < BUCKET_SEARCH_LIMIT) {
                String chatRoomBucket = Message.buildChatRoomBucket(chatRoomId, currentBucket);
                Optional<Message> message = queryLastMessageInBucket(chatRoomBucket);

                if (message.isPresent()) {
                    log.debug("Found last message in bucket: {}", currentBucket);
                    return message;
                }

                // Move to previous day
                currentBucket = getPreviousBucket(currentBucket);
                daysSearched++;
            }

            log.debug("No messages found for chatroom: {} in last {} days",
                     chatRoomId, BUCKET_SEARCH_LIMIT);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error finding last message for chatroom: {}", chatRoomId, e);
            return Optional.empty();
        }
    }

    private Optional<Message> queryLastMessageInBucket(String chatRoomBucket) {
        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder()
                        .partitionValue(chatRoomBucket)
                        .build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .scanIndexForward(false) // Descending order (newest first)
                .limit(1)
                .build();

        Iterator<Message> results = messageTable.query(queryRequest).items().iterator();

        if (results.hasNext()) {
            Message message = results.next();
            // Skip hidden messages
            if (Boolean.TRUE.equals(message.getIsHidden())) {
                // Try next message
                if (results.hasNext()) {
                    return Optional.of(results.next());
                }
                return Optional.empty();
            }
            return Optional.of(message);
        }

        return Optional.empty();
    }

    /**
     * Save a new message
     * @param message the message to save
     * @return the saved message
     */
    public Message saveMessage(Message message) {
        try {
            messageTable.putItem(message);
            log.debug("Message saved: messageId={}", message.getMessageId());
            return message;
        } catch (Exception e) {
            log.error("Error saving message: {}", message.getMessageId(), e);
            throw new RuntimeException("Failed to save message", e);
        }
    }

//    /**
//     * Update an existing message
//     * @param message the message to update
//     * @return the updated message
//     */
//    public Message updateMessage(Message message) {
//        try {
//            DynamoDbTable<Message> table = getMessagesTable();
//            Message updated = table.updateItem(message);
//            log.debug("Message updated: messageId={}", message.getMessageId());
//            return updated;
//        } catch (Exception e) {
//            log.error("Error updating message: {}", message.getMessageId(), e);
//            throw new RuntimeException("Failed to update message", e);
//        }
//    }
//
//    /**
//     * Find a message by its composite key
//     * @param conversationBucket the partition key
//     * @param messageSk the sort key
//     * @return Optional containing the message
//     */
//    public Optional<Message> findMessageByKey(String conversationBucket, String messageSk) {
//        try {
//            DynamoDbTable<Message> table = getMessagesTable();
//
//            Key key = Key.builder()
//                    .partitionValue(conversationBucket)
//                    .sortValue(messageSk)
//                    .build();
//
//            Message message = table.getItem(key);
//            return Optional.ofNullable(message);
//
//        } catch (Exception e) {
//            log.error("Error finding message by key: bucket={}, sk={}", conversationBucket, messageSk, e);
//            return Optional.empty();
//        }
//    }
//
//
    /**
     * Find messages in a specific bucket
     * @param chatRoomId the chatroom ID
     * @param bucket the date bucket (yyyyMMdd)
     * @param scanIndexForward true for ascending, false for descending
     * @return list of messages
     */
    public List<Message> findMessagesByBucket(String chatRoomId, String bucket, boolean scanIndexForward) {
        try {
            String conversationBucket = Message.buildChatRoomBucket(chatRoomId, bucket);
            QueryConditional queryConditional = QueryConditional
                    .keyEqualTo(Key.builder()
                            .partitionValue(conversationBucket)
                            .build());

            QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                    .queryConditional(queryConditional)
                    .scanIndexForward(scanIndexForward)
                    .limit(DEFAULT_LIMIT)
                    .build();

            return messageTable.query(queryRequest)
                    .items()
                    .stream()
                    .sorted(Comparator.comparing(Message::getCreatedAt))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error finding messages by bucket: chatRoomId={}, bucket={}",
                     chatRoomId, bucket, e);
            return List.of();
        }
    }
//
//    // ========== PinnedMessage Operations ==========
//
//    /**
//     * Save a pinned message
//     * @param pinnedMessage the pinned message to save
//     * @return the saved pinned message
//     */
//    public PinnedMessage savePinnedMessage(PinnedMessage pinnedMessage) {
//        try {
//            DynamoDbTable<PinnedMessage> table = getPinnedMessagesTable();
//            table.putItem(pinnedMessage);
//            log.debug("Pinned message saved: chatRoomId={}, messageId={}",
//                     pinnedMessage.getChatRoomId(), pinnedMessage.getMessageId());
//            return pinnedMessage;
//        } catch (Exception e) {
//            log.error("Error saving pinned message: chatRoomId={}, messageId={}",
//                     pinnedMessage.getChatRoomId(), pinnedMessage.getMessageId(), e);
//            throw new RuntimeException("Failed to save pinned message", e);
//        }
//    }
//
//    /**
//     * Delete a pinned message by its composite key
//     * @param chatRoomId the chatroom ID (PK)
//     * @param pinnedSk the pinned sort key (SK)
//     */
//    public void deletePinnedMessage(String chatRoomId, String pinnedSk) {
//        try {
//            DynamoDbTable<PinnedMessage> table = getPinnedMessagesTable();
//
//            Key key = Key.builder()
//                    .partitionValue(chatRoomId)
//                    .sortValue(pinnedSk)
//                    .build();
//
//            table.deleteItem(key);
//            log.debug("Pinned message deleted: chatRoomId={}, pinnedSk={}", chatRoomId, pinnedSk);
//
//        } catch (Exception e) {
//            log.error("Error deleting pinned message: chatRoomId={}, pinnedSk={}",
//                     chatRoomId, pinnedSk, e);
//            throw new RuntimeException("Failed to delete pinned message", e);
//        }
//    }
//
//    /**
//     * Find a pinned message by chatroom and message ID
//     * @param chatRoomId the chatroom ID
//     * @param messageId the message ID
//     * @return Optional containing the pinned message
//     */
//    public Optional<PinnedMessage> findPinnedMessageByConversationAndMessageId(String chatRoomId, String messageId) {
//        try {
//            List<PinnedMessage> allPinned = findAllPinnedMessagesByConversation(chatRoomId);
//
//            return allPinned.stream()
//                    .filter(pm -> messageId.equals(pm.getMessageId()))
//                    .findFirst();
//
//        } catch (Exception e) {
//            log.error("Error finding pinned message: chatRoomId={}, messageId={}",
//                     chatRoomId, messageId, e);
//            return Optional.empty();
//        }
//    }
//
//    /**
//     * Find all pinned messages for a chatroom
//     * @param chatRoomId the chatroom ID
//     * @return list of pinned messages, sorted by pinnedAt descending
//     */
//    public List<PinnedMessage> findAllPinnedMessagesByConversation(String chatRoomId) {
//        try {
//            DynamoDbTable<PinnedMessage> table = getPinnedMessagesTable();
//
//            QueryConditional queryConditional = QueryConditional
//                    .keyEqualTo(Key.builder()
//                            .partitionValue(chatRoomId)
//                            .build());
//
//            QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
//                    .queryConditional(queryConditional)
//                    .scanIndexForward(false) // Newest pins first
//                    .build();
//
//            return table.query(queryRequest)
//                    .items()
//                    .stream()
//                    .collect(Collectors.toList());
//
//        } catch (Exception e) {
//            log.error("Error finding all pinned messages for chatroom: {}", chatRoomId, e);
//            return List.of();
//        }
//    }
//
//    /**
//     * Check if a message is pinned in a chatroom
//     * @param chatRoomId the chatroom ID
//     * @param messageId the message ID
//     * @return true if pinned, false otherwise
//     */
//    public boolean existsPinnedMessage(String chatRoomId, String messageId) {
//        return findPinnedMessageByConversationAndMessageId(chatRoomId, messageId).isPresent();
//    }

    // ========== Helper Methods ==========

    private String getCurrentBucket() {
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        return now.format(BUCKET_FORMATTER);
    }

    private String getPreviousBucket(String bucket) {
        try {
            LocalDate date = LocalDate.parse(bucket, BUCKET_FORMATTER);
            LocalDate previousDate = date.minusDays(1);
            return previousDate.format(BUCKET_FORMATTER);
        } catch (Exception e) {
            log.error("Error parsing bucket date: {}", bucket, e);
            return bucket;
        }
    }
}