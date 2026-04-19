package iuh.fit.goat.config;

import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.MessageHidden;
import iuh.fit.goat.entity.PinnedMessage;
import iuh.fit.goat.entity.Poll;
import iuh.fit.goat.entity.PollVote;
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

    @Value("${dynamodb.table.message_hidden}")
    private String messageHiddenTableName;

    @Value("${dynamodb.table.polls}")
    private String pollsTableName;

    @Value("${dynamodb.table.poll_votes}")
    private String pollVotesTableName;

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

    @Bean
    @Scope("prototype")
    public DynamoDbTable<MessageHidden> messageHiddenTable() {
        TableSchema<MessageHidden> schema = TableSchema.fromBean(MessageHidden.class);
        return enhancedClient.table(messageHiddenTableName, schema);
    }

    @Bean
    @Scope("prototype")
    public DynamoDbTable<Poll> pollTable() {
        TableSchema<Poll> schema = TableSchema.fromBean(Poll.class);
        return enhancedClient.table(pollsTableName, schema);
    }

    @Bean
    @Scope("prototype")
    public DynamoDbTable<PollVote> pollVoteTable() {
        TableSchema<PollVote> schema = TableSchema.fromBean(PollVote.class);
        return enhancedClient.table(pollVotesTableName, schema);
    }
}