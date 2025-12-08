package iuh.fit.goat.config.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.goat.entity.Notification;
import iuh.fit.goat.service.NotificationService;
import iuh.fit.goat.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisKeyExpirationListener implements MessageListener {

    private final RedisService redisService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // Lấy ra expired key từ message và kiểm tra
        String expiredKey = message.getBody() != null
                ? new String(message.getBody(), StandardCharsets.UTF_8)
                : null;

        if (expiredKey == null || !expiredKey.startsWith("notification:event:")) {
            return;
        }

        log.info("Expired notification event key: {}", expiredKey);

        // Extract notification ID from key (format: "notification:event:<id>")
        // Lấy ra UUID của notification từ key (được gen ra, cái này không phải là Id trong database)
        String id = expiredKey.replace("notification:event:", "");

        // Lấy ra data từ UUID key đã lưu trong redis để check
        String dataKey = "notification:data:" + id;
        String payload = redisService.getValue(dataKey);

        if (payload == null) {
            log.warn("No data found for notification {}", id);
            return;
        }

        // Map ra notification
        try {
            // Parse payload
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);

            // Save notification to DB
            Notification saved = notificationService.createNotification(
                    notificationService.buildNotification(data)
            );

            // Send via WebSocket in NotificationService
            notificationService.sendNotificationToUser(saved.getRecipient(), saved);

            log.info("Notification {} saved and sent to user {}", saved.getNotificationId(), saved.getRecipient().getContact().getEmail());

            // Clean up data key
            redisService.deleteKey(dataKey);

        } catch (Exception e) {
            log.error("Failed to process expired notification {}: {}", id, e.getMessage(), e);
        }
    }
}
