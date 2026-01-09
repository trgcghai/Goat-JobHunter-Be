package iuh.fit.goat.component.redis.application;

import iuh.fit.goat.dto.result.application.ApplicationCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ApplicationEventProducer {

    private final RedisTemplate<String, Object> redisTemplate;

    @Async
    public void publishApplicationCreated(ApplicationCreatedEvent application) {
        try {
            Map<String, Object> message = new HashMap<>();

            message.put("eventType", "APPLICATION_CREATED");
            message.put("applicationId", application.getApplicationId());
            message.put("applicantEmail", application.getApplicantEmail());
            message.put("applicantName", application.getApplicantName());
            message.put("companyEmail", application.getCompanyEmail());
            message.put("companyName", application.getCompanyName());
            message.put("jobTitle", application.getJobTitle());
            message.put("retry", "0");
            message.put("createdAt", Instant.now().toString());

            this.redisTemplate.opsForStream().add("application.events", message);
        } catch (Exception  e) {
            throw new RuntimeException("Cannot publish application event", e);
        }
    }
}
