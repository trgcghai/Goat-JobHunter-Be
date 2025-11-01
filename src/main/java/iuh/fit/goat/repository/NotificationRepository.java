package iuh.fit.goat.repository;

import iuh.fit.goat.common.NotificationType;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Notification;
import iuh.fit.goat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {
    Optional<Notification> findByTypeAndActorAndBlogAndRecipient(
            NotificationType type,
            User actor,
            Blog blog,
            User recipient
    );
}
