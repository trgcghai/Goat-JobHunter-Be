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
@Order(2)
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "migration.message-bucket", name = "enabled", havingValue = "true")
public class MessageBucketPatternMigrationRunner implements CommandLineRunner {

    private final DynamoDbClient dynamoDbClient;

    @Value("${migration.message-bucket.legacy-table:}")
    private String legacyTableName;

    @Value("${migration.message-bucket.target-table:${dynamodb.table.messages}}")
    private String targetTableName;

    @Value("${migration.message-bucket.force:false}")
    private boolean forceMigration;

    @Override
    public void run(String... args) {
        if (legacyTableName == null || legacyTableName.isBlank()) {
            log.warn("Bucket-pattern migration skipped: legacy table name is empty");
            return;
        }

        if (legacyTableName.equals(targetTableName)) {
            log.warn("Bucket-pattern migration skipped: legacy table and target table are the same ({})", targetTableName);
            return;
        }

        if (!forceMigration && targetTableHasData()) {
            log.info("Bucket-pattern migration skipped: target table {} already has data (set migration.message-bucket.force=true to override)",
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
        log.info("Starting message bucket-pattern migration from {} to {}", legacyTableName, targetTableName);

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
                    log.info("Migrated {} messages so far", migratedCount);
                }
            }

            lastEvaluatedKey = response.lastEvaluatedKey();
        } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

        log.info("Bucket-pattern migration completed: migrated={}, skipped={}", migratedCount, skippedCount);
    }

    private Map<String, AttributeValue> transformItem(Map<String, AttributeValue> legacyItem) {
        if (legacyItem == null || legacyItem.isEmpty()) {
            return null;
        }

        Map<String, AttributeValue> transformed = new HashMap<>(legacyItem);

        if (!transformed.containsKey("chatRoomId") || transformed.get("chatRoomId").s() == null
                || transformed.get("chatRoomId").s().isBlank()) {
            String chatRoomId = extractChatRoomIdFromBucket(transformed.get("chatRoomBucket"));
            if (chatRoomId == null) {
                log.warn("Skipping message because chatRoomId cannot be derived: messageId={}",
                        transformed.containsKey("messageId") ? transformed.get("messageId").s() : "unknown");
                return null;
            }
            transformed.put("chatRoomId", AttributeValue.builder().s(chatRoomId).build());
        }

        transformed.remove("chatRoomBucket");
        transformed.remove("bucket");

        return transformed;
    }

    private String extractChatRoomIdFromBucket(AttributeValue chatRoomBucket) {
        if (chatRoomBucket == null || chatRoomBucket.s() == null || chatRoomBucket.s().isBlank()) {
            return null;
        }

        String raw = chatRoomBucket.s();
        int separator = raw.indexOf('#');
        if (separator <= 0) {
            return null;
        }

        return raw.substring(0, separator);
    }
}
