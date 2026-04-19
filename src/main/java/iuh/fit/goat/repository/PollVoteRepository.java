package iuh.fit.goat.repository;

import iuh.fit.goat.entity.PollVote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.List;
import java.util.Optional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class PollVoteRepository {
    private final DynamoDbTable<PollVote> pollVoteTable;

    public PollVote save(PollVote vote) {
        try {
            this.pollVoteTable.putItem(vote);
            return vote;
        } catch (Exception e) {
            log.error("Error saving vote: {}", vote.getVoteId(), e);
            throw new RuntimeException("Failed to save vote", e);
        }
    }

    public Optional<PollVote> findByVoteId(String voteId) {
        if (voteId == null || voteId.isBlank()) return Optional.empty();
        try {
            Key key = Key.builder().partitionValue(voteId).build();
            PollVote vote = this.pollVoteTable.getItem(key);
            return Optional.ofNullable(vote);
        } catch (Exception e) {
            log.error("Error finding vote by ID: {}", voteId, e);
            throw new RuntimeException("Failed to find vote", e);
        }
    }

    public List<PollVote> findByPollIdAndAccountId(String pollId, Long accountId) {
        if (pollId == null || pollId.isBlank() || accountId == null) return List.of();

        try {
            return this.pollVoteTable.scan()
                    .items()
                    .stream()
                    .filter(v -> v.getPollId().equals(pollId) && v.getAccountId().equals(accountId))
                    .toList();
        } catch (Exception e) {
            log.error("Error finding votes for poll {} and user {}", pollId, accountId, e);
            throw new RuntimeException("Failed to find votes", e);
        }
    }

    public List<PollVote> findByPollId(String pollId) {
        if (pollId == null || pollId.isBlank()) return List.of();
        try {
            return this.pollVoteTable.scan()
                    .items()
                    .stream()
                    .filter(v -> v.getPollId().equals(pollId))
                    .toList();
        } catch (Exception e) {
            log.error("Error finding votes for poll {}", pollId, e);
            throw new RuntimeException("Failed to find votes", e);
        }
    }

    public List<PollVote> findByOptionId(String optionId) {
        if (optionId == null || optionId.isBlank()) return List.of();
        try {
            return this.pollVoteTable.scan()
                    .items()
                    .stream()
                    .filter(v -> v.getOptionId().equals(optionId))
                    .toList();
        } catch (Exception e) {
            log.error("Error finding votes for option {}", optionId, e);
            throw new RuntimeException("Failed to find votes", e);
        }
    }

    public void delete(String voteId) {
        if (voteId == null || voteId.isBlank()) return;
        try {
            Key key = Key.builder().partitionValue(voteId).build();
            this.pollVoteTable.deleteItem(key);
            log.info("Vote deleted: {}", voteId);
        } catch (Exception e) {
            log.error("Error deleting vote: {}", voteId, e);
            throw new RuntimeException("Failed to delete vote", e);
        }
    }

    public void deleteByPollId(String pollId) {
        if (pollId == null || pollId.isBlank()) return;
        try {
            List<PollVote> votes = findByPollId(pollId);
            for (PollVote vote : votes) {
                delete(vote.getVoteId());
            }
            log.info("Deleted {} votes for poll: {}", votes.size(), pollId);
        } catch (Exception e) {
            log.error("Error deleting votes for poll {}", pollId, e);
            throw new RuntimeException("Failed to delete votes", e);
        }
    }
}