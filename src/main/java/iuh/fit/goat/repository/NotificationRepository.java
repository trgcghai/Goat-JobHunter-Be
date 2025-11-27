package iuh.fit.goat.repository;

import iuh.fit.goat.common.NotificationType;
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
    Optional<Notification> findByTypeAndActorAndBlogAndRecipient(
            NotificationType type,
            User actor,
            Blog blog,
            User recipient
    );

    Page<Notification> findByRecipient_UserId(Long userId, Pageable pageable);

    List<Notification> findByNotificationIdInAndRecipient_UserId(List<Long> notificationIds, Long userId);

    List<Notification> findByRecipient_UserIdOrderByCreatedAtDesc(Long userId);
}
