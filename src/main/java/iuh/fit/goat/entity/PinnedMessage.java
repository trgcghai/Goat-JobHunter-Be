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
    private String pinnedSk;        // SK: PIN#<pinnedAt>#<messageId>

    private String messageId;
    private String messageBucket;
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
    @DynamoDbAttribute("pinnedSk")
    public String getPinnedSk() {
        return pinnedSk;
    }

    @DynamoDbAttribute("messageId")
    public String getMessageId() {
        return messageId;
    }

    @DynamoDbAttribute("messageBucket")
    public String getMessageBucket() {
        return messageBucket;
    }

    @DynamoDbAttribute("pinnedBy")
    public String getPinnedBy() {
        return pinnedBy;
    }

    @DynamoDbAttribute("pinnedAt")
    public Instant getPinnedAt() {
        return pinnedAt;
    }

    // Helper method: Generate pinnedSk from pinnedAt and messageId
    // Format: PIN#<pinnedAt>#<messageId>
    // Example: PIN#1706501000000#msg_abc123
    public static String buildPinnedSk(Long pinnedAt, String messageId) {
        return "PIN#" + pinnedAt + "#" + messageId;
    }

    // Helper method: Extract pinnedAt timestamp from pinnedSk
    public Long extractPinnedAt() {
        if (pinnedSk == null || !pinnedSk.startsWith("PIN#")) {
            return null;
        }
        String[] parts = pinnedSk.split("#");
        if (parts.length >= 2) {
            try {
                return Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // Helper method: Extract messageId from pinnedSk
    public String extractMessageId() {
        if (pinnedSk == null || !pinnedSk.startsWith("PIN#")) {
            return null;
        }
        String[] parts = pinnedSk.split("#");
        if (parts.length >= 3) {
            return parts[2];
        }
        return null;
    }
}