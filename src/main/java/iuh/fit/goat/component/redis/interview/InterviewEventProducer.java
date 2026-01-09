package iuh.fit.goat.component.redis.interview;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.goat.dto.response.interview.InterviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class InterviewEventProducer {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Async
    public void publishInterviewCreated(String email, List<InterviewResponse> interviews, String reason) {
        try {
            Map<String, Object> message = new HashMap<>();

            message.put("eventType", "INTERVIEW_CREATED");
            message.put("email", email);
            message.put("interviews", this.objectMapper.writeValueAsString(interviews));
            message.put("reason", reason);
            message.put("retry", "0");
            message.put("createdAt", Instant.now().toString());

            this.redisTemplate.opsForStream().add("interview.events", message);
        } catch (Exception  e) {
            throw new RuntimeException("Cannot publish interview event", e);
        }
    }
}
