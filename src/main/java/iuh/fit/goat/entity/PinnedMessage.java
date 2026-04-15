package iuh.fit.goat.entity;

import iuh.fit.goat.util.SecurityUtil;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;

@DynamoDbBean
@Getter
@Setter
@AllArgsConstructor
@Builder
public class PinnedMessage {

    private String chatRoomId;
    private String messageId;
    private String pinnedBy;
    private Instant pinnedAt;

    public PinnedMessage() {
        this.pinnedAt = Instant.now();
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("chatRoomId")
    public String getChatRoomId() {
        return chatRoomId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("messageId")
    public String getMessageId() {
        return messageId;
    }

    @DynamoDbAttribute("pinnedBy")
    public String getPinnedBy() {
        return pinnedBy;
    }

    @DynamoDbAttribute("pinnedAt")
    public Instant getPinnedAt() {
        return pinnedAt;
    }

}