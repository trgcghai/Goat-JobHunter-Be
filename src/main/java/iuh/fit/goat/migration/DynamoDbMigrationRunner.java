package iuh.fit.goat.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class DynamoDbMigrationRunner implements CommandLineRunner {

    private final DynamoDbClient dynamoDbClient;

    @Override
    public void run(String... args) {
        log.info("🚀 Starting DynamoDB migration...");

        createMessagesTable();
        createPinnedMessagesTable();

        log.info("✅ DynamoDB migration completed successfully!");
    }

    private void createMessagesTable() {
        String tableName = "messages";

        if (tableExists(tableName)) {
            log.info("📦 Table '{}' already exists, skipping creation", tableName);
            return;
        }

        log.info("🔨 Creating table '{}'...", tableName);

        CreateTableRequest request = CreateTableRequest.builder()
                .tableName(tableName)
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("chatRoomBucket")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("messageSk")
                                .attributeType(ScalarAttributeType.S)
                                .build()
                )
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName("chatRoomBucket")
                                .keyType(KeyType.HASH)
                                .build(),
                        KeySchemaElement.builder()
                                .attributeName("messageSk")
                                .keyType(KeyType.RANGE)
                                .build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        try {
            dynamoDbClient.createTable(request);
            waitForTableCreation(tableName);
            log.info("✅ Table '{}' created successfully", tableName);
        } catch (Exception e) {
            log.error("❌ Failed to create table '{}'", tableName, e);
            throw new RuntimeException("Failed to create messages table", e);
        }
    }

    private void createPinnedMessagesTable() {
        String tableName = "pinned_messages";

        if (tableExists(tableName)) {
            log.info("📦 Table '{}' already exists, skipping creation", tableName);
            return;
        }

        log.info("🔨 Creating table '{}'...", tableName);

        CreateTableRequest request = CreateTableRequest.builder()
                .tableName(tableName)
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("chatRoomId")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("pinnedSk")
                                .attributeType(ScalarAttributeType.S)
                                .build()
                )
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName("chatRoomId")
                                .keyType(KeyType.HASH)
                                .build(),
                        KeySchemaElement.builder()
                                .attributeName("pinnedSk")
                                .keyType(KeyType.RANGE)
                                .build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        try {
            dynamoDbClient.createTable(request);
            waitForTableCreation(tableName);
            log.info("✅ Table '{}' created successfully", tableName);
        } catch (Exception e) {
            log.error("❌ Failed to create table '{}'", tableName, e);
            throw new RuntimeException("Failed to create pinned_messages table", e);
        }
    }

    private boolean tableExists(String tableName) {
        try {
            DescribeTableRequest request = DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build();

            DescribeTableResponse response = dynamoDbClient.describeTable(request);

            TableStatus status = response.table().tableStatus();
            log.debug("Table '{}' exists with status: {}", tableName, status);

            return true;
        } catch (ResourceNotFoundException e) {
            log.debug("Table '{}' does not exist", tableName);
            return false;
        } catch (Exception e) {
            log.error("Error checking if table '{}' exists", tableName, e);
            throw new RuntimeException("Failed to check table existence", e);
        }
    }

    private void waitForTableCreation(String tableName) {
        try {
            log.info("⏳ Waiting for table '{}' to become active...", tableName);

            int maxAttempts = 30;
            int attempt = 0;

            while (attempt < maxAttempts) {
                DescribeTableRequest request = DescribeTableRequest.builder()
                        .tableName(tableName)
                        .build();

                DescribeTableResponse response = dynamoDbClient.describeTable(request);
                TableStatus status = response.table().tableStatus();

                if (status == TableStatus.ACTIVE) {
                    log.info("✅ Table '{}' is now active", tableName);
                    return;
                }

                log.debug("Table '{}' status: {}, waiting...", tableName, status);
                Thread.sleep(2000);
                attempt++;
            }

            throw new RuntimeException("Table creation timeout for: " + tableName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Table creation interrupted", e);
        }
    }
}