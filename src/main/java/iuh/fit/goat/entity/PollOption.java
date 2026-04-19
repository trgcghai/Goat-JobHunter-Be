package iuh.fit.goat.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;

@DynamoDbBean
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PollOption {
    private String optionId;
    private String text;
    private String createdBy;
    private Instant createdAt;
    private Integer voteCount;

    @DynamoDbAttribute("optionId")
    public String getOptionId() {
        return optionId;
    }

    @DynamoDbAttribute("text")
    public String getText() {
        return text;
    }

    @DynamoDbAttribute("createdBy")
    public String getCreatedBy() {
        return createdBy;
    }

    @DynamoDbAttribute("createdAt")
    public Instant getCreatedAt() {
        return createdAt;
    }

    @DynamoDbAttribute("voteCount")
    public Integer getVoteCount() {
        return voteCount;
    }
}

