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
        message.getBody();
        String expiredKey = new String(message.getBody(), StandardCharsets.UTF_8);

        if (!expiredKey.startsWith("notification:") || !expiredKey.endsWith(":listener")) {
            return;
        }


        log.info("Expired notification event key: {}", expiredKey);

        try {
            String dataKey = expiredKey.replace(":listener", "");
            String payload = redisService.getValue(dataKey);

            if (payload == null) {
                log.warn("No data found for notification key {}", dataKey);
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);

            // Build and save notification
            Notification saved = notificationService.createNotification(
                    notificationService.buildNotification(data)
            );

            // Send via WebSocket
            notificationService.sendNotificationToUser(saved.getRecipient(), saved);

            if(redisService.hasKey(dataKey)) {
                redisService.deleteKey(dataKey);
            }
        } catch (Exception e) {
            log.error("Failed to process expired notification {}: {}", expiredKey, e.getMessage(), e);
        }
    }
}
