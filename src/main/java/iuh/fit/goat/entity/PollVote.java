package iuh.fit.goat.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;

@DynamoDbBean
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PollVote {
    private String voteId;
    private String pollId;
    private String optionId;
    private Long accountId;
    private Instant createdAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("voteId")
    public String getVoteId() {
        return voteId;
    }

    @DynamoDbAttribute("pollId")
    public String getPollId() {
        return pollId;
    }

    @DynamoDbAttribute("optionId")
    public String getOptionId() {
        return optionId;
    }

    @DynamoDbAttribute("accountId")
    public Long getAccountId() {
        return accountId;
    }

    @DynamoDbAttribute("createdAt")
    public Instant getCreatedAt() {
        return createdAt;
    }
}

