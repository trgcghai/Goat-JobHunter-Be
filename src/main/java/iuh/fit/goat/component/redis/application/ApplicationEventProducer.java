package iuh.fit.goat.component.redis.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.goat.dto.result.application.ApplicationCreatedEvent;
import iuh.fit.goat.dto.result.application.ApplicationStatusEvent;
import iuh.fit.goat.entity.Application;
import iuh.fit.goat.enumeration.Status;
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
public class ApplicationEventProducer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

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

    @Async
    public void publishApplicationStatus(String email, String username, List<ApplicationStatusEvent> applications, String reason, String status) {
        try {
            Map<String, Object> message = new HashMap<>();

            message.put("eventType", "APPLICATION_STATUS");
            message.put("email", email);
            message.put("username", username);
            message.put("applications", this.objectMapper.writeValueAsString(applications));
            message.put("status", status);
            message.put("reason", reason);
            message.put("retry", "0");
            message.put("createdAt", Instant.now().toString());

            this.redisTemplate.opsForStream().add("application.events", message);
        } catch (Exception  e) {
            throw new RuntimeException("Cannot publish application event", e);
        }
    }
}
