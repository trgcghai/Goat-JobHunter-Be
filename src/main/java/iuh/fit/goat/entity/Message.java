package iuh.fit.goat.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;

@DynamoDbBean
@Getter
@Setter
@AllArgsConstructor
@Builder
public class Message {

    private String chatRoomBucket; // PK: <chatRoomId>#<bucket>
    private String messageSk;          // SK: MSG#<timestamp>#<messageId>

    private String chatRoomId;
    private String bucket;
    private String messageId;
    private String senderId;
    private String content;
    private String messageType;        // TEXT | FILES | CARD
    private String replyTo;            // nullable
    private Boolean isHidden;
    private Instant createdAt;
    private Instant updatedAt;

    public Message() {
        this.createdAt = Instant.now();
    }

    // Partition Key
    @DynamoDbPartitionKey
    @DynamoDbAttribute("chatRoomBucket")
    public String getConversationBucket() {
        return chatRoomBucket;
    }

    // Sort Key
    @DynamoDbSortKey
    @DynamoDbAttribute("messageSk")
    public String getMessageSk() {
        return messageSk;
    }

    @DynamoDbAttribute("chatRoomId")
    public String getChatRoomId() {
        return chatRoomId;
    }

    @DynamoDbAttribute("bucket")
    public String getBucket() {
        return bucket;
    }

    @DynamoDbAttribute("messageId")
    public String getMessageId() {
        return messageId;
    }

    @DynamoDbAttribute("senderId")
    public String getSenderId() {
        return senderId;
    }

    @DynamoDbAttribute("content")
    public String getContent() {
        return content;
    }

    @DynamoDbAttribute("messageType")
    public String getMessageType() {
        return messageType;
    }

    @DynamoDbAttribute("replyTo")
    public String getReplyTo() {
        return replyTo;
    }

    @DynamoDbAttribute("isHidden")
    public Boolean getIsHidden() {
        return isHidden;
    }

    @DynamoDbAttribute("createdAt")
    public Instant getCreatedAt() {
        return createdAt;
    }

    @DynamoDbAttribute("updatedAt")
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Helper method: Generate chatRoomBucket from chatRoomId and bucket
    // Format: <chatRoomId>#<bucket>
    // Example: conv_123#20240129
    public static String buildChatRoomBucket(String chatRoomId, String bucket) {
        return chatRoomId + "#" + bucket;
    }

    // Helper method: Generate messageSk from timestamp and messageId
    // Format: MSG#<timestamp>#<messageId>
    // Example: MSG#1706500000123#msg_abc123
    public static String buildMessageSk(Long timestamp, String messageId) {
        return "MSG#" + timestamp + "#" + messageId;
    }

    // Helper method: Extract timestamp from messageSk
    public Long extractTimestamp() {
        if (messageSk == null || !messageSk.startsWith("MSG#")) {
            return null;
        }
        String[] parts = messageSk.split("#");
        if (parts.length >= 2) {
            try {
                return Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}