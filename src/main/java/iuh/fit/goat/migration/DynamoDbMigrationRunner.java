package iuh.fit.goat.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;

@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class DynamoDbMigrationRunner implements CommandLineRunner {

    private final DynamoDbClient dynamoDbClient;

    @Value("${dynamodb.table.messages}")
    private String messagesTableName;

    @Value("${dynamodb.table.pinned_messages}")
    private String pinnedMessagesTableName;

    @Override
    public void run(String... args) {
        log.info("🚀 Starting DynamoDB migration...");

        createMessagesTable();
        createPinnedMessagesTable();

        log.info("✅ DynamoDB migration completed successfully!");
    }

    private void createMessagesTable() {
        String tableName = messagesTableName;
        List<KeySchemaElement> expectedSchema = List.of(
                KeySchemaElement.builder().attributeName("chatRoomId").keyType(KeyType.HASH).build(),
                KeySchemaElement.builder().attributeName("messageSk").keyType(KeyType.RANGE).build()
        );

        if (tableExistsAndValidateSchema(tableName, expectedSchema)) {
            log.info("📦 Table '{}' already exists with expected schema, skipping creation", tableName);
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
                                .attributeName("messageSk")
                                .attributeType(ScalarAttributeType.S)
                                .build()
                )
                .keySchema(
                        KeySchemaElement.builder()
                        .attributeName("chatRoomId")
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
        String tableName = pinnedMessagesTableName;
        List<KeySchemaElement> expectedSchema = List.of(
                KeySchemaElement.builder().attributeName("chatRoomId").keyType(KeyType.HASH).build(),
                KeySchemaElement.builder().attributeName("messageId").keyType(KeyType.RANGE).build()
        );

        if (tableExistsAndValidateSchema(tableName, expectedSchema)) {
            log.info("📦 Table '{}' already exists with expected schema, skipping creation", tableName);
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
                        .attributeName("messageId")
                                .attributeType(ScalarAttributeType.S)
                                .build()
                )
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName("chatRoomId")
                                .keyType(KeyType.HASH)
                                .build(),
                        KeySchemaElement.builder()
                            .attributeName("messageId")
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

    private boolean tableExistsAndValidateSchema(String tableName, List<KeySchemaElement> expectedSchema) {
        if (!tableExists(tableName)) {
            return false;
        }

        DescribeTableResponse response = dynamoDbClient.describeTable(
                DescribeTableRequest.builder().tableName(tableName).build()
        );

        List<KeySchemaElement> actualSchema = response.table().keySchema();
        if (!sameKeySchema(actualSchema, expectedSchema)) {
            String actual = formatKeySchema(actualSchema);
            String expected = formatKeySchema(expectedSchema);
            String message = "Schema mismatch for table '" + tableName + "'. Expected " + expected +
                    " but found " + actual + ". Use a new table name and migrate data before cutover.";
            log.error(message);
            throw new IllegalStateException(message);
        }

        return true;
    }

    private boolean sameKeySchema(List<KeySchemaElement> actual, List<KeySchemaElement> expected) {
        if (actual == null || expected == null || actual.size() != expected.size()) {
            return false;
        }

        for (KeySchemaElement expectedElement : expected) {
            boolean found = actual.stream().anyMatch(a ->
                    a.keyType() == expectedElement.keyType()
                            && a.attributeName().equals(expectedElement.attributeName()));
            if (!found) {
                return false;
            }
        }

        return true;
    }

    private String formatKeySchema(List<KeySchemaElement> schema) {
        if (schema == null || schema.isEmpty()) {
            return "[]";
        }

        return schema.stream()
                .map(s -> s.keyType().toString() + "=" + s.attributeName())
                .sorted()
                .toList()
                .toString();
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