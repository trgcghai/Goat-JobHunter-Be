package iuh.fit.goat.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;
import java.util.List;

@DynamoDbBean
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Poll {
    private String pollId;
    private Long chatRoomId;
    private String messageId;
    private String createdBy;
    private String question;
    private List<PollOption> options;
    private Boolean multipleChoice;
    private Boolean allowAddOption;
    private Boolean pinned;
    private Boolean isClosed;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("pollId")
    public String getPollId() {
        return pollId;
    }

    @DynamoDbAttribute("chatRoomId")
    public Long getChatRoomId() {
        return chatRoomId;
    }

    @DynamoDbAttribute("messageId")
    public String getMessageId() {
        return messageId;
    }

    @DynamoDbAttribute("createdBy")
    public String getCreatedBy() {
        return createdBy;
    }

    @DynamoDbAttribute("question")
    public String getQuestion() {
        return question;
    }

    @DynamoDbAttribute("options")
    public List<PollOption> getOptions() {
        return options;
    }

    @DynamoDbAttribute("multipleChoice")
    public Boolean getMultipleChoice() {
        return multipleChoice;
    }

    @DynamoDbAttribute("allowAddOption")
    public Boolean getAllowAddOption() {
        return allowAddOption;
    }

    @DynamoDbAttribute("pinned")
    public Boolean getPinned() {
        return pinned;
    }

    @DynamoDbAttribute("isClosed")
    public Boolean getIsClosed() {
        return isClosed;
    }

    @DynamoDbAttribute("expiresAt")
    public Instant getExpiresAt() {
        return expiresAt;
    }

    @DynamoDbAttribute("createdAt")
    public Instant getCreatedAt() {
        return createdAt;
    }

    @DynamoDbAttribute("updatedAt")
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

