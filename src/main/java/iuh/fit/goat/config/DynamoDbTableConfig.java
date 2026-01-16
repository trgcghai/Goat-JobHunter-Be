package iuh.fit.goat.config;

import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.PinnedMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Configuration
public class DynamoDbTableConfig {

    @Value("${dynamodb.table.messages}")
    private String messagesTableName;

    @Value("${dynamodb.table.pinned_messages}")
    private String pinnedMessagesTableName;

    private final DynamoDbEnhancedClient enhancedClient;

    public DynamoDbTableConfig(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    @Bean
    @Scope("prototype")
    public DynamoDbTable<Message> messageTable() {
        // Create fresh TableSchema each time to avoid classloader issues
        TableSchema<Message> schema = TableSchema.fromBean(Message.class);
        return enhancedClient.table(messagesTableName, schema);
    }

    @Bean
    @Scope("prototype")
    public DynamoDbTable<PinnedMessage> pinnedMessageTable() {
        TableSchema<PinnedMessage> schema = TableSchema.fromBean(PinnedMessage.class);
        return enhancedClient.table(pinnedMessagesTableName, schema);
    }
}