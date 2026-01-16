package iuh.fit.goat.component.redis.interview;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.goat.common.Role;
import iuh.fit.goat.dto.response.interview.InterviewResponse;
import iuh.fit.goat.dto.result.interview.InterviewFeedbackEvent;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.EmailNotificationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewEmailConsumer {
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailNotificationService emailService;
    private final ObjectMapper objectMapper;

    private static final String STREAM = "interview.events";
    private static final String DLQ_STREAM = "interview.events.dlq";
    private static final String GROUP = "interview-service-group";
    private static final String CONSUMER =
            "email-service-" + System.getenv().getOrDefault("HOSTNAME", UUID.randomUUID().toString());
    private static final int MAX_RETRY = 5;

    @PostConstruct
    public void init() throws InvalidException {
        try {
            boolean groupExists = this.redisTemplate
                    .opsForStream()
                    .groups(STREAM)
                    .stream()
                    .anyMatch(g -> GROUP.equals(g.groupName()));

            if (!groupExists) {
                this.redisTemplate.opsForStream()
                        .createGroup(STREAM, ReadOffset.from("0-0"), GROUP);
            }
        } catch (Exception ignored) {
            throw new InvalidException("Failed to create Redis Stream group");
        }
    }

    @Scheduled(fixedDelay = 3000)
    public void poll() {
        consume();
        claim();
    }

    // Tự động claim các message bị bỏ quên sau 30 giây và đưa về consumer hiện tại xử lý
    private void claim() {
        PendingMessages pending = this.redisTemplate.opsForStream().pending(STREAM, GROUP, Range.unbounded(), 10);

        if (pending.isEmpty()) return;

        List<RecordId> ids = pending.get().toList().stream()
                .filter(p -> p.getElapsedTimeSinceLastDelivery().compareTo(Duration.ofSeconds(30)) >= 0)
                .filter(p -> p.getTotalDeliveryCount() < MAX_RETRY)
                .map(PendingMessage::getId)
                .toList();

        if (!ids.isEmpty()) {
            this.redisTemplate.opsForStream().claim(
                    STREAM,
                    GROUP,
                    CONSUMER,
                    Duration.ofSeconds(30),
                    ids.toArray(new RecordId[0])
            );
        }
    }

    private void consume() {
        List<MapRecord<String, Object, Object>> records =
                this.redisTemplate.opsForStream().read(
                        Consumer.from(GROUP, CONSUMER),
                        StreamReadOptions.empty().count(10),
                        StreamOffset.create(STREAM, ReadOffset.lastConsumed())
                );

        if (records == null || records.isEmpty()) return;

        for (MapRecord<String, Object, Object> myRecord : records) {
            handleEmail(myRecord);
        }
    }

    private void handleEmail(MapRecord<String, Object, Object> myRecord) {
        Map<Object, Object> value = myRecord.getValue();
        int retry = Integer.parseInt(value.get("retry").toString());
        String eventType = value.get("eventType").toString();

        try {
            if(eventType.equals("INTERVIEW_CREATED")) {
                Object interviews = value.get("interviews");
                if(interviews != null) {
                    this.emailService.handleSendInterviewEmailToApplicant(
                            value.get("email").toString(),
                            this.objectMapper.readValue(interviews.toString(), new TypeReference<List<InterviewResponse>>() {}),
                            value.get("reason").toString()
                    );
                }
            } else {
                Object interview = value.get("interview");
                if(interview != null) {
                    this.emailService.handleSendFeedbackInterviewEmailToApplicantOrCompany(
                            value.get("applicantEmail").toString(),
                            this.objectMapper.readValue(value.get("interview").toString(), InterviewFeedbackEvent.class),
                            Role.APPLICANT
                    );

                    this.emailService.handleSendFeedbackInterviewEmailToApplicantOrCompany(
                            value.get("companyEmail").toString(),
                            this.objectMapper.readValue(value.get("interview").toString(), InterviewFeedbackEvent.class),
                            Role.COMPANY
                    );
                }
            }

            this.redisTemplate.opsForStream().acknowledge(STREAM, GROUP, myRecord.getId());

        } catch (Exception e) {
            log.error("Send mail failed", e);

            if (retry >= MAX_RETRY) {
                sendToDLQ(myRecord); // đưa vào Dead Letter Queue
            } else {
                retryMessage(myRecord, retry);
            }
        }
    }

    private void sendToDLQ(MapRecord<String, Object, Object> myRecord) {
        this.redisTemplate.opsForStream().add(DLQ_STREAM, myRecord.getValue());

        this.redisTemplate.opsForStream().acknowledge(STREAM, GROUP, myRecord.getId());

        log.warn("Message moved to DLQ: {}", myRecord.getId());
    }

    private void retryMessage(MapRecord<String, Object, Object> myRecord, int retry) {
        Map<String, String> newData = new HashMap<>();
        myRecord.getValue().forEach((k, v) ->
                newData.put(k.toString(), v.toString())
        );
        newData.put("retry", String.valueOf(retry + 1));

        this.redisTemplate.opsForStream().add(STREAM, newData);

        this.redisTemplate.opsForStream().acknowledge(STREAM, GROUP, myRecord.getId());
    }
}
