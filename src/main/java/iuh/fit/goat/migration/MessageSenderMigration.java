package iuh.fit.goat.migration;

import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.entity.embeddable.SenderInfo;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Migration script to populate sender information in existing messages
 * Run once with profile: spring.profiles.active=migration
 * Then remove @Component annotation or set enabled=false
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSenderMigration implements CommandLineRunner {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    // Set to false after successful migration
    private static final boolean MIGRATION_ENABLED = true;

    @Override
    public void run(String... args) {
        if (!MIGRATION_ENABLED) {
            log.info("Message sender migration is disabled");
            return;
        }

        log.info("Starting message sender migration...");

        try {
            migrateAllMessages();
            log.info("Message sender migration completed successfully");
        } catch (Exception e) {
            log.error("Message sender migration failed", e);
            throw new RuntimeException("Migration failed", e);
        }
    }

    private void migrateAllMessages() {
        // Scan all messages from DynamoDB
        List<Message> allMessages = messageRepository.scanAllMessages();

        log.info("Found {} messages to migrate", allMessages.size());

        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;

        for (Message message : allMessages) {
            try {
                // Skip if already migrated
                if (message.getSender() != null) {
                    skippedCount++;
                    continue;
                }

                // Skip if no senderId
                if (message.getSenderId() == null || message.getSenderId().isBlank()) {
                    log.warn("Message {} has no senderId, skipping", message.getMessageId());
                    skippedCount++;
                    continue;
                }

                // Fetch user information
                Long senderId = Long.parseLong(message.getSenderId());
                Optional<User> userOpt = userRepository.findById(senderId);

                if (userOpt.isEmpty()) {
                    log.warn("User {} not found for message {}, using fallback",
                            senderId, message.getMessageId());

                    // Use fallback sender info
                    SenderInfo fallbackSender = createFallbackSender(senderId);
                    message.setSender(fallbackSender);
                } else {
                    // Build sender info from user
                    User user = userOpt.get();
                    SenderInfo senderInfo = SenderInfo.builder()
                            .accountId(user.getAccountId())
                            .fullName(user.getFullName())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .avatar(user.getAvatar())
                            .build();

                    message.setSender(senderInfo);
                }

                // Save updated message
                messageRepository.saveMessage(message);
                successCount++;

                // Log progress every 100 messages
                if (successCount % 100 == 0) {
                    log.info("Migrated {} messages so far...", successCount);
                }

            } catch (Exception e) {
                log.error("Failed to migrate message {}: {}",
                        message.getMessageId(), e.getMessage());
                failedCount++;
            }
        }

        log.info("Migration complete - Success: {}, Failed: {}, Skipped: {}",
                successCount, failedCount, skippedCount);
    }

    private SenderInfo createFallbackSender(Long accountId) {
        return SenderInfo.builder()
                .accountId(accountId)
                .fullName("Người dùng không tồn tại")
                .username("deleted_user")
                .email("deleted@example.com")
                .avatar(null)
                .build();
    }
}