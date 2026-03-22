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

    private String chatRoomId;  // PK
    private String messageId;
    private String pinnedBy;
    private Instant pinnedAt;

    public PinnedMessage() {
        this.pinnedAt = Instant.now();
        this.pinnedBy = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "System";
    }

    // Partition Key
    @DynamoDbPartitionKey
    @DynamoDbAttribute("chatRoomId")
    public String getChatRoomId() {
        return chatRoomId;
    }

    // Sort Key
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