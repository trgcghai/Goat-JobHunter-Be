package iuh.fit.goat.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.HashMap;
import java.util.Map;

@Component
@Order(3)
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "migration.pinned-message", name = "enabled", havingValue = "true")
public class PinnedMessageKeyMigrationRunner implements CommandLineRunner {

    private final DynamoDbClient dynamoDbClient;

    @Value("${migration.pinned-message.legacy-table:}")
    private String legacyTableName;

    @Value("${migration.pinned-message.target-table:${dynamodb.table.pinned_messages}}")
    private String targetTableName;

    @Value("${migration.pinned-message.force:false}")
    private boolean forceMigration;

    @Override
    public void run(String... args) {
        if (legacyTableName == null || legacyTableName.isBlank()) {
            log.warn("Pinned-message migration skipped: legacy table name is empty");
            return;
        }

        if (legacyTableName.equals(targetTableName)) {
            log.warn("Pinned-message migration skipped: legacy table and target table are the same ({})", targetTableName);
            return;
        }

        if (!forceMigration && targetTableHasData()) {
            log.info("Pinned-message migration skipped: target table {} already has data (set migration.pinned-message.force=true to override)",
                    targetTableName);
            return;
        }

        migrateAllItems();
    }

    private boolean targetTableHasData() {
        ScanRequest request = ScanRequest.builder()
                .tableName(targetTableName)
                .limit(1)
                .build();

        ScanResponse response = dynamoDbClient.scan(request);
        return response.hasItems() && !response.items().isEmpty();
    }

    private void migrateAllItems() {
        log.info("Starting pinned-message migration from {} to {}", legacyTableName, targetTableName);

        Map<String, AttributeValue> lastEvaluatedKey = null;
        long migratedCount = 0;
        long skippedCount = 0;

        do {
            ScanRequest.Builder scanBuilder = ScanRequest.builder()
                    .tableName(legacyTableName)
                    .limit(200);

            if (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty()) {
                scanBuilder.exclusiveStartKey(lastEvaluatedKey);
            }

            ScanResponse response = dynamoDbClient.scan(scanBuilder.build());

            for (Map<String, AttributeValue> legacyItem : response.items()) {
                Map<String, AttributeValue> transformed = transformItem(legacyItem);
                if (transformed == null) {
                    skippedCount++;
                    continue;
                }

                PutItemRequest putRequest = PutItemRequest.builder()
                        .tableName(targetTableName)
                        .item(transformed)
                        .build();

                dynamoDbClient.putItem(putRequest);
                migratedCount++;

                if (migratedCount % 500 == 0) {
                    log.info("Migrated {} pinned messages so far", migratedCount);
                }
            }

            lastEvaluatedKey = response.lastEvaluatedKey();
        } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

        log.info("Pinned-message migration completed: migrated={}, skipped={}", migratedCount, skippedCount);
    }

    private Map<String, AttributeValue> transformItem(Map<String, AttributeValue> legacyItem) {
        if (legacyItem == null || legacyItem.isEmpty()) {
            return null;
        }

        Map<String, AttributeValue> transformed = new HashMap<>(legacyItem);

        String chatRoomId = extractString(transformed.get("chatRoomId"));
        if (chatRoomId == null || chatRoomId.isBlank()) {
            return null;
        }

        String messageId = extractString(transformed.get("messageId"));
        if (messageId == null || messageId.isBlank()) {
            messageId = extractMessageIdFromPinnedSk(transformed.get("pinnedSk"));
            if (messageId == null || messageId.isBlank()) {
                return null;
            }
            transformed.put("messageId", AttributeValue.builder().s(messageId).build());
        }

        transformed.remove("pinnedSk");
        transformed.remove("messageBucket");

        return transformed;
    }

    private String extractString(AttributeValue value) {
        if (value == null || value.s() == null || value.s().isBlank()) {
            return null;
        }
        return value.s();
    }

    private String extractMessageIdFromPinnedSk(AttributeValue pinnedSk) {
        String raw = extractString(pinnedSk);
        if (raw == null) {
            return null;
        }

        int separator = raw.lastIndexOf('#');
        if (separator < 0 || separator == raw.length() - 1) {
            return null;
        }

        return raw.substring(separator + 1);
    }
}