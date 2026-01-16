package iuh.fit.goat.config.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;

import javax.annotation.PostConstruct;

@Component
@Slf4j
@RequiredArgsConstructor
public class DynamoConnectionChecker {
    private final DynamoDbClient dynamoDbClient;

    @PostConstruct
    public void checkConnection() {
        try {
            ListTablesResponse response = dynamoDbClient.listTables();
            log.info("✅ DynamoDB connected successfully!");
            log.info("📦 Tables: {}", response.tableNames());
        } catch (Exception e) {
            log.error("❌ DynamoDB connection failed!", e);
        }
    }
}
