package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.PinnedMessage;
import iuh.fit.goat.enumeration.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.time.Instant;
import java.util.*;

@Repository
@Slf4j
@RequiredArgsConstructor
public class MessageRepository {

    private static final int DEFAULT_LIMIT = 100;
    private static final int DEFAULT_SEARCH_PAGE_SIZE = 20;
    private static final int DEFAULT_SEARCH_SCAN_LIMIT = 3000;
    private static final int MAX_SEARCH_SCAN_LIMIT = 10000;
    private static final int SEARCH_QUERY_PAGE_SIZE = 100;

    private final DynamoDbTable<Message> messageTable;
    private final DynamoDbTable<PinnedMessage> pinnedMessageTable;

    public record MessageSearchResult(
            List<Message> messages,
            long matchedCount,
            boolean scanLimitReached
    ) {
    }

    /**
     * Scan all messages (for migration only)
     * WARNING: Expensive operation - use only during migration
     */
    public List<Message> scanAllMessages() {
        return messageTable.scan()
                .items()
                .stream()
                .toList();
    }

    /**
     * Find all forwarded descendants of a root message ID.
     *
     * This uses a full table scan because current schema has no secondary index on originalMessageId.
     */
    public List<Message> findForwardedDescendantsByOriginalMessageId(String rootMessageId) {
        if (rootMessageId == null || rootMessageId.isBlank()) {
            return Collections.emptyList();
        }

        List<Message> allMessages = scanAllMessages();
        Map<String, List<Message>> childrenByOriginalMessageId = new HashMap<>();

        for (Message message : allMessages) {
            if (message == null || message.getMessageId() == null || message.getMessageId().isBlank()) {
                continue;
            }

            if (!Boolean.TRUE.equals(message.getIsForwarded())) {
                continue;
            }

            String originalMessageId = message.getOriginalMessageId();
            if (originalMessageId == null || originalMessageId.isBlank()) {
                continue;
            }

            childrenByOriginalMessageId
                    .computeIfAbsent(originalMessageId, ignored -> new ArrayList<>())
                    .add(message);
        }

        List<Message> descendants = new ArrayList<>();
        Set<String> visitedMessageIds = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();

        visitedMessageIds.add(rootMessageId);
        queue.add(rootMessageId);

        while (!queue.isEmpty()) {
            String parentMessageId = queue.poll();
            List<Message> children = childrenByOriginalMessageId
                    .getOrDefault(parentMessageId, Collections.emptyList());

            for (Message child : children) {
                String childMessageId = child.getMessageId();
                if (childMessageId == null || childMessageId.isBlank()) {
                    continue;
                }

                if (!visitedMessageIds.add(childMessageId)) {
                    continue;
                }

                descendants.add(child);
                queue.add(childMessageId);
            }
        }

        return descendants;
    }

    // ========== Message Query Methods ==========

    /**
         * Find messages in a chat room sorted by newest first.
     */
    public List<Message> findMessagesByChatRoom(
            String chatRoomId,
            int limit,
            boolean includeHidden
    ) {
        return queryMessagesByChatRoom(chatRoomId, limit, includeHidden);
    }

    public MessageSearchResult searchMessagesByChatRoom(
            String chatRoomId,
            String searchTerm,
            int pageNumber,
            int pageSize,
            int scanLimit
    ) {
        if (chatRoomId == null || chatRoomId.isBlank() || searchTerm == null || searchTerm.isBlank()) {
            return new MessageSearchResult(Collections.emptyList(), 0, false);
        }

        int safePageNumber = Math.max(pageNumber, 0);
        int safePageSize = pageSize > 0 ? pageSize : DEFAULT_SEARCH_PAGE_SIZE;
        int safeScanLimit = resolveSearchScanLimit(scanLimit, safePageSize);
        long offset = (long) safePageNumber * safePageSize;
        String normalizedTerm = searchTerm.toLowerCase(Locale.ROOT);

        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(chatRoomId).build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .scanIndexForward(false)
                .limit(Math.min(SEARCH_QUERY_PAGE_SIZE, safeScanLimit))
                .build();

        List<Message> pagedMessages = new ArrayList<>(safePageSize);
        long matchedCount = 0;
        int scannedCount = 0;
        boolean scanLimitReached = false;

        outer:
        for (Page<Message> page : messageTable.query(queryRequest)) {
            for (Message message : page.items()) {
                if (scannedCount >= safeScanLimit) {
                    scanLimitReached = true;
                    break outer;
                }

                scannedCount++;

                if (!matchesSearchTerm(message, normalizedTerm)) {
                    continue;
                }

                if (matchedCount < offset) {
                    matchedCount++;
                    continue;
                }

                if (pagedMessages.size() < safePageSize) {
                    pagedMessages.add(message);
                }

                matchedCount++;
            }
        }

        return new MessageSearchResult(pagedMessages, matchedCount, scanLimitReached);
    }

    /**
         * Query messages from a specific chat room partition.
     */
    private List<Message> queryMessagesByChatRoom(
        String chatRoomId,
        int limit,
        boolean includeHidden
    ) {

        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder()
                .partitionValue(chatRoomId)
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

    public long countUnreadMessages(String chatRoomId, String lastReadMessageSk, String currentAccountId) {
        QueryEnhancedRequest request;

        if (lastReadMessageSk == null || lastReadMessageSk.isBlank()) {
            request = QueryEnhancedRequest.builder()
                    .queryConditional(
                            QueryConditional.keyEqualTo(
                                    Key.builder().partitionValue(chatRoomId).build()
                            )
                    )
                    .build();
        } else {
            request = QueryEnhancedRequest.builder()
                    .queryConditional(
                            QueryConditional.sortGreaterThan(
                                    Key.builder()
                                        .partitionValue(chatRoomId)
                                        .sortValue(lastReadMessageSk)
                                        .build()
                            )
                    )
                    .build();
        }

        PageIterable<Message> pages = this.messageTable.query(request);
        long count = 0;
        for (Page<Message> page : pages) {
            for(Message message : page.items()) {
                if(message.getSender() != null && message.getSender().getAccountId() != null && !message.getSender().getAccountId().toString().equals(currentAccountId)) {
                    count++;
                }
            }
        }

        return count;
    }

    private int resolveSearchScanLimit(int requestedScanLimit, int pageSize) {
        int effectiveRequestedLimit = requestedScanLimit > 0 ? requestedScanLimit : DEFAULT_SEARCH_SCAN_LIMIT;
        int boundedLimit = Math.min(effectiveRequestedLimit, MAX_SEARCH_SCAN_LIMIT);
        return Math.max(boundedLimit, pageSize);
    }

    private boolean matchesSearchTerm(Message message, String normalizedTerm) {
        if (message == null || Boolean.TRUE.equals(message.getIsHidden())) {
            return false;
        }

        String content = message.getContent();
        if (content == null || content.isBlank()) {
            return false;
        }

        return content.toLowerCase(Locale.ROOT).contains(normalizedTerm);
    }

    /**
     * Find the last (newest) non-system message in a chat room.
     */
    public Optional<Message> findLastMessageByConversation(String chatRoomId) {
        try {
            Optional<Message> lastMessage = queryLastMessageByChatRoom(chatRoomId);
            if (lastMessage.isEmpty()) {
                log.info("No messages found for chatRoom: {}", chatRoomId);
            }
            return lastMessage;

        } catch (Exception e) {
            log.error("Error finding last message for chatRoom: {}", chatRoomId, e);
            return Optional.empty();
        }
    }

    private Optional<Message> queryLastMessageByChatRoom(String chatRoomId) {
        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder()
                        .partitionValue(chatRoomId)
                        .build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .scanIndexForward(false)
            .limit(10) // Get enough rows to skip system messages
                .build();

        Iterator<Message> results = messageTable.query(queryRequest).items().iterator();

        while (results.hasNext()) {
            Message message = results.next();
            // Keep hidden (revoked) messages for preview, skip system messages only
            if (message.getMessageType() != MessageType.SYSTEM) {
                return Optional.of(message);
            }
        }

        return Optional.empty();
    }

    /**
     * Find a message by chat room and message ID.
     */
    public Optional<Message> findByChatRoomIdAndMessageId(String chatRoomId, String messageId) {
        if (chatRoomId == null || chatRoomId.isBlank() || messageId == null || messageId.isBlank()) {
            return Optional.empty();
        }

        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(
                        Key.builder()
                        .partitionValue(chatRoomId)
                        .build()
                );

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .scanIndexForward(false)
                .build();

        for (Message message : messageTable.query(queryRequest).items()) {
            if (messageId.equals(message.getMessageId())) {
                return Optional.of(message);
            }
        }

        return Optional.empty();
    }

    // ========== Message Operations ==========

    public Message saveMessage(Message message) {
        try {
            messageTable.putItem(message);
            log.info("Message saved successfully: chatRoomId={}, SK={}",
                    message.getChatRoomId(), message.getMessageSk());
            return message;
        } catch (Exception e) {
            log.error("Error saving message: chatRoomId={}, SK={}",
                    message.getChatRoomId(), message.getMessageSk(), e);
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public void deleteMessage(String chatRoomId, String messageSk) {
        if (chatRoomId == null || chatRoomId.isBlank() || messageSk == null || messageSk.isBlank()) {
            throw new IllegalArgumentException("chatRoomId and messageSk are required");
        }

        try {
            Key key = Key.builder()
                    .partitionValue(chatRoomId)
                    .sortValue(messageSk)
                    .build();

            messageTable.deleteItem(key);
            log.info("Message deleted successfully: chatRoomId={}, SK={}", chatRoomId, messageSk);
        } catch (Exception e) {
            log.error("Error deleting message: chatRoomId={}, SK={}", chatRoomId, messageSk, e);
            throw new RuntimeException("Failed to delete message", e);
        }
    }

    // ========== Pinned Message Methods ==========
    public PinnedMessage pinMessage(String chatRoomId, String messageId, String pinnedBy) {
        if (chatRoomId == null || chatRoomId.isBlank() || messageId == null || messageId.isBlank()) {
            throw new RuntimeException("chatRoomId and messageId are required");
        }

        try {
            PinnedMessage pinnedMessage = PinnedMessage.builder()
                    .chatRoomId(chatRoomId)
                    .messageId(messageId)
                    .pinnedBy(pinnedBy != null ? pinnedBy : "System")
                    .pinnedAt(Instant.now())
                    .build();

            this.pinnedMessageTable.putItem(pinnedMessage);
            return pinnedMessage;
        } catch (Exception e) {
            throw new RuntimeException("Failed to pin message", e);
        }
    }

    public void deletePinnedMessage(String chatRoomId, String messageId) {
        if (chatRoomId == null || chatRoomId.isBlank() || messageId == null || messageId.isBlank()) {
            return;
        }

        try {
            Key key = Key.builder()
                    .partitionValue(chatRoomId)
                    .sortValue(messageId)
                    .build();

            pinnedMessageTable.deleteItem(key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to cleanup pinned message", e);
        }
    }

    public List<PinnedMessage> getPinnedMessagesByChatRoom(String chatRoomId) {
        if (chatRoomId == null || chatRoomId.isBlank()) {
            return Collections.emptyList();
        }

        try {
            QueryConditional conditional = QueryConditional
                    .keyEqualTo(Key.builder().partitionValue(chatRoomId).build());

            QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                    .queryConditional(conditional)
                    .build();

            return pinnedMessageTable.query(request)
                    .items()
                    .stream()
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve pinned messages", e);
        }
    }

    public boolean isPinned(String chatRoomId, String messageId) {
        if (chatRoomId == null || chatRoomId.isBlank() || messageId == null || messageId.isBlank()) {
            return false;
        }

        try {
            Key key = Key.builder()
                    .partitionValue(chatRoomId)
                    .sortValue(messageId)
                    .build();

            PinnedMessage pinnedMessage = this.pinnedMessageTable.getItem(key);
            return pinnedMessage != null;
        } catch (Exception e) {
            return false;
        }
    }

    public PinnedMessage getPinnedMessage(String chatRoomId, String messageId) {
        if (chatRoomId == null || chatRoomId.isBlank() || messageId == null || messageId.isBlank()) {
            return null;
        }

        try {
            Key key = Key.builder()
                    .partitionValue(chatRoomId)
                    .sortValue(messageId)
                    .build();

            return pinnedMessageTable.getItem(key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve pinned message", e);
        }
    }
}
