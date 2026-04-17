package iuh.fit.goat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@DynamoDbBean
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageHidden {

    private String messageId;
    private Long userId;
    private Instant hiddenAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("messageId")
    public String getMessageId() {
        return messageId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("userId")
    public Long getUserId() {
        return userId;
    }

    @DynamoDbAttribute("hiddenAt")
    public Instant getHiddenAt() {
        return hiddenAt;
    }
}
