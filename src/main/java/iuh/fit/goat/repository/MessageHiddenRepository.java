package iuh.fit.goat.repository;

import iuh.fit.goat.entity.MessageHidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Repository
@Slf4j
@RequiredArgsConstructor
public class MessageHiddenRepository {

    private static final int BATCH_GET_MAX_SIZE = 100;

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<MessageHidden> messageHiddenTable;

    public void hideMessageForUser(String messageId, Long userId, Instant hiddenAt) {
        if (messageId == null || messageId.isBlank() || userId == null) {
            throw new IllegalArgumentException("messageId and userId are required");
        }

        Instant effectiveHiddenAt = hiddenAt != null ? hiddenAt : Instant.now();

        MessageHidden item = MessageHidden.builder()
                .messageId(messageId)
                .userId(userId)
                .hiddenAt(effectiveHiddenAt)
                .build();

        messageHiddenTable.putItem(item);
    }

    public Set<String> findHiddenMessageIdsForUser(List<String> messageIds, Long userId) {
        if (messageIds == null || messageIds.isEmpty() || userId == null) {
            return Collections.emptySet();
        }

        List<String> normalizedMessageIds = messageIds.stream()
                .filter(messageId -> messageId != null && !messageId.isBlank())
                .distinct()
                .toList();

        if (normalizedMessageIds.isEmpty()) {
            return Collections.emptySet();
        }

        return findHiddenMessageIds(normalizedMessageIds, userId);
    }

    private Set<String> findHiddenMessageIds(List<String> messageIds, Long userId) {
        Set<String> hiddenMessageIds = new LinkedHashSet<>();

        for (int index = 0; index < messageIds.size(); index += BATCH_GET_MAX_SIZE) {
            int endIndex = Math.min(index + BATCH_GET_MAX_SIZE, messageIds.size());
            List<String> chunk = new ArrayList<>(messageIds.subList(index, endIndex));

            ReadBatch.Builder<MessageHidden> readBatchBuilder = ReadBatch.builder(MessageHidden.class)
                    .mappedTableResource(messageHiddenTable);

            for (String messageId : chunk) {
                Key key = Key.builder()
                        .partitionValue(messageId)
                        .sortValue(userId)
                        .build();

                readBatchBuilder.addGetItem(key);
            }

            BatchGetItemEnhancedRequest request = BatchGetItemEnhancedRequest.builder()
                    .readBatches(readBatchBuilder.build())
                    .build();

            BatchGetResultPageIterable results = enhancedClient.batchGetItem(request);
            for (var page : results) {
                page.resultsForTable(messageHiddenTable).forEach(item -> {
                    if (item != null && item.getMessageId() != null && !item.getMessageId().isBlank()) {
                        hiddenMessageIds.add(item.getMessageId());
                    }
                });
            }
        }

        return hiddenMessageIds;
    }
}
