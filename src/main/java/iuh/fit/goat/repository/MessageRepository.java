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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
@RequiredArgsConstructor
public class MessageRepository {

    private static final DateTimeFormatter BUCKET_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int BUCKET_SEARCH_LIMIT = 30;
    private static final int DEFAULT_LIMIT = 100;

    private final DynamoDbTable<Message> messageTable;
    private final DynamoDbTable<PinnedMessage> pinnedMessageTable;

    // ========== NEW: Multi-Bucket Query Methods ==========

    /**
     * Find messages across multiple buckets with smart fallback
     * Strategy:
     * 1. Start from today's bucket
     * 2. If not enough messages, fetch from previous buckets
     * 3. Merge and sort by timestamp DESC
     */
    public List<Message> findMessagesAcrossBuckets(
            String chatRoomId,
            int limit,
            boolean includeHidden) {

        List<Message> allMessages = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();

        // Search up to 30 days back or until we have enough messages
        for (int daysBack = 0; daysBack < BUCKET_SEARCH_LIMIT && allMessages.size() < limit; daysBack++) {
            LocalDate targetDate = currentDate.minusDays(daysBack);
            String bucket = targetDate.format(BUCKET_FORMATTER);
            String chatRoomBucket = Message.buildChatRoomBucket(chatRoomId, bucket);

            log.debug("Querying bucket: {} for chatRoom: {}", bucket, chatRoomId);

            List<Message> bucketMessages = queryMessagesInBucket(
                    chatRoomBucket,
                    limit - allMessages.size(),
                    includeHidden
            );

            if (!bucketMessages.isEmpty()) {
                log.info("Found {} messages in bucket: {}", bucketMessages.size(), bucket);
                allMessages.addAll(bucketMessages);
            }
        }

        // Sort by timestamp DESC and apply limit
        return allMessages.stream()
                .sorted(Comparator.comparing(Message::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Query messages from a specific bucket
     */
    private List<Message> queryMessagesInBucket(
            String chatRoomBucket,
            int limit,
            boolean includeHidden) {

        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder()
                        .partitionValue(chatRoomBucket)
                        .build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .scanIndexForward(false) // Descending order (newest first)
                .limit(limit)
                .build();

        List<Message> messages = new ArrayList<>();
        messageTable.query(queryRequest).items().forEach(message -> {
            // Filter hidden messages unless explicitly requested
            if (includeHidden || !Boolean.TRUE.equals(message.getIsHidden())) {
                messages.add(message);
            }
        });

        return messages;
    }

    /**
     * Count messages in a specific bucket
     */
    public long countMessagesInBucket(String chatRoomId, String bucket) {
        String chatRoomBucket = Message.buildChatRoomBucket(chatRoomId, bucket);

        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder()
                        .partitionValue(chatRoomBucket)
                        .build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .scanIndexForward(false)
                .build();

        long count = 0;
        for (Message ignored : messageTable.query(queryRequest).items()) {
            count++;
        }

        log.debug("Bucket {} has {} messages", bucket, count);
        return count;
    }

    /**
     * Get the most recent bucket that contains messages
     */
    public Optional<String> findLatestNonEmptyBucket(String chatRoomId) {
        LocalDate currentDate = LocalDate.now();

        for (int daysBack = 0; daysBack < BUCKET_SEARCH_LIMIT; daysBack++) {
            LocalDate targetDate = currentDate.minusDays(daysBack);
            String bucket = targetDate.format(BUCKET_FORMATTER);

            long messageCount = countMessagesInBucket(chatRoomId, bucket);
            if (messageCount > 0) {
                log.info("Found latest non-empty bucket: {} with {} messages", bucket, messageCount);
                return Optional.of(bucket);
            }
        }

        log.warn("No non-empty buckets found for chatRoom: {}", chatRoomId);
        return Optional.empty();
    }

    // ========== UPDATED: Last Message Query ==========

    /**
     * Find the last (newest) message with aggressive multi-bucket fallback
     */
    public Optional<Message> findLastMessageByConversation(String chatRoomId) {
        try {
            LocalDate currentDate = LocalDate.now();

            // Search up to 30 days back (increased from 7)
            for (int daysBack = 0; daysBack < BUCKET_SEARCH_LIMIT; daysBack++) {
                LocalDate targetDate = currentDate.minusDays(daysBack);
                String bucket = targetDate.format(BUCKET_FORMATTER);
                String chatRoomBucket = Message.buildChatRoomBucket(chatRoomId, bucket);

                Optional<Message> lastMessage = queryLastMessageInBucket(chatRoomBucket);
                if (lastMessage.isPresent()) {
                    log.debug("Found last message in bucket: {}", bucket);
                    return lastMessage;
                }
            }

            log.info("No messages found in any bucket for chatRoom: {}", chatRoomId);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error finding last message for chatRoom: {}", chatRoomId, e);
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
                .scanIndexForward(false)
                .limit(5) // Get top 5 to skip hidden messages
                .build();

        Iterator<Message> results = messageTable.query(queryRequest).items().iterator();

        while (results.hasNext()) {
            Message message = results.next();
            // Skip hidden messages
            if (!Boolean.TRUE.equals(message.getIsHidden())) {
                return Optional.of(message);
            }
        }

        return Optional.empty();
    }

    // ========== EXISTING: Message Operations ==========

    public List<Message> findMessagesByBucket(
            String chatRoomId,
            String bucket,
            boolean includeHidden) {

        String chatRoomBucket = Message.buildChatRoomBucket(chatRoomId, bucket);
        return queryMessagesInBucket(chatRoomBucket, DEFAULT_LIMIT, includeHidden);
    }

    public Message saveMessage(Message message) {
        try {
            messageTable.putItem(message);
            log.info("Message saved successfully: PK={}, SK={}",
                    message.getChatRoomBucket(), message.getMessageSk());
            return message;
        } catch (Exception e) {
            log.error("Error saving message: PK={}, SK={}",
                    message.getChatRoomBucket(), message.getMessageSk(), e);
            throw new RuntimeException("Failed to save message", e);
        }
    }

    // ========== Helper Methods ==========

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