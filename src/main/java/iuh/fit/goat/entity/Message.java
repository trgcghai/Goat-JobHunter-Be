package iuh.fit.goat.entity;

import iuh.fit.goat.entity.embeddable.SenderInfo;
import iuh.fit.goat.enumeration.MessageType;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;

@DynamoDbBean
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Message {

    private String chatRoomBucket;
    private String messageSk;
    private String chatRoomId;
    private String bucket;
    private String messageId;

    // NEW: Embedded sender information
    private SenderInfo sender;

    // DEPRECATED: Keep for backward compatibility during migration
    @Deprecated
    private String senderId;

    private String content;
    private MessageType messageType;
    private String replyTo;
    private Boolean isHidden;
    private Instant createdAt;
    private Instant updatedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("chatRoomBucket")
    public String getChatRoomBucket() {
        return chatRoomBucket;
    }

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

    @DynamoDbAttribute("sender")
    public SenderInfo getSender() {
        return sender;
    }

    @Deprecated
    @DynamoDbAttribute("senderId")
    public String getSenderId() {
        return senderId;
    }

    @DynamoDbAttribute("content")
    public String getContent() {
        return content;
    }

    @DynamoDbAttribute("messageType")
    public MessageType getMessageType() {
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

    public static String buildChatRoomBucket(String chatRoomId, String bucket) {
        return chatRoomId + "#" + bucket;
    }

    public static String buildMessageSk(Long timestamp, String messageId) {
        return "MSG#" + timestamp + "#" + messageId;
    }

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