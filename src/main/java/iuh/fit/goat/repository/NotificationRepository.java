package iuh.fit.goat.repository;

import iuh.fit.goat.enumeration.NotificationType;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Notification;
import iuh.fit.goat.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {
    Optional<Notification> findByTypeAndActorsContainingAndBlogAndRecipient(
            NotificationType type,
            User actor,
            Blog blog,
            User recipient
    );

    Optional<Notification> findByTypeAndBlogAndRecipient(
            NotificationType type,
            Blog blog,
            User recipient
    );

    Optional<Notification> findByTypeAndRecipient(
            NotificationType type,
            User recipient
    );

    Page<Notification> findByRecipient_AccountId(Long accountId, Pageable pageable);

    List<Notification> findByNotificationIdInAndRecipient_AccountId(List<Long> notificationIds, Long accountId);

    List<Notification> findByRecipient_AccountIdOrderByCreatedAtDesc(Long userId);

    Optional<Notification> findByTypeAndActorsContainingAndRecipient(
            NotificationType type,
            User actor,
            User recipient
    );
}
