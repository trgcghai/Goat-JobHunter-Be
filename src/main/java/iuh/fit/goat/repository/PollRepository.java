package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Poll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Optional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class PollRepository {
    private final DynamoDbTable<Poll> pollTable;

    public Poll save(Poll poll) {
        try {
            this.pollTable.putItem(poll);
            return poll;
        } catch (Exception e) {
            log.error("Error saving poll: {}", poll.getPollId(), e);
            throw new RuntimeException("Failed to save poll", e);
        }
    }

    public Optional<Poll> findByPollId(String pollId) {
        if (pollId == null || pollId.isBlank()) return Optional.empty();
        try {
            Key key = Key.builder().partitionValue(pollId).build();
            Poll poll = this.pollTable.getItem(key);
            return Optional.ofNullable(poll);
        } catch (Exception e) {
            log.error("Error finding poll by ID: {}", pollId, e);
            throw new RuntimeException("Failed to find poll", e);
        }
    }

    public List<Poll> findByChatRoomId(String chatRoomId) {
        if (chatRoomId == null || chatRoomId.isBlank()) return List.of();
        try {
            return this.pollTable.query(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(chatRoomId).build())
            ).items().stream().toList();
        } catch (Exception e) {
            log.error("Error finding polls by chat room: {}", chatRoomId, e);
            throw new RuntimeException("Failed to find polls", e);
        }
    }

    public void delete(String pollId) {
        if (pollId == null || pollId.isBlank()) return;
        try {
            Key key = Key.builder().partitionValue(pollId).build();
            this.pollTable.deleteItem(key);
            log.info("Poll deleted: {}", pollId);
        } catch (Exception e) {
            log.error("Error deleting poll: {}", pollId, e);
            throw new RuntimeException("Failed to delete poll", e);
        }
    }
}

