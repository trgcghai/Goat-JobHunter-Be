package iuh.fit.goat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.goat.common.NotificationType;
import iuh.fit.goat.dto.response.notification.NotificationResponse;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Comment;
import iuh.fit.goat.entity.Notification;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.repository.NotificationRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.NotificationService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private final ObjectMapper objectMapper;

    private final Sinks.Many<Notification> notificationSink = Sinks.many().multicast().onBackpressureBuffer();

    private void handlePushTo(Notification notification) {
        this.notificationSink.tryEmitNext(notification);
    }

    private User handleGetCurrentUser() {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        return this.userRepository.findByContact_Email(currentEmail);
    }

    @Override
    public Flux<ServerSentEvent<String>> stream() {
        User currentUser = this.handleGetCurrentUser();
        if(currentUser == null) {
            return Flux.empty();
        }

        return this.notificationSink.asFlux()
                .filter(n ->
                        {
                            if (n.getRecipient() == null) return false;
                            return Objects.equals(
                                    n.getRecipient().getUserId(),
                                    currentUser.getUserId()
                            );
                        }
                )
                .<ServerSentEvent<String>>handle((n, sink) -> {
                            try {
                                NotificationResponse response = this.covertToNotificationResponse(n);
                                String jsonData = this.objectMapper.writeValueAsString(response);
                                sink.next(ServerSentEvent.<String>builder()
                                        .event("notification-event")
                                        .data(jsonData)
                                        .build()
                                );
                            } catch (JsonProcessingException e) {
                                sink.error(new RuntimeException("Error serializing notification", e));
                            }
                        })
                .doOnError(Throwable::printStackTrace)
                .onErrorResume(e -> Flux.empty());
    }

    @Override
    public List<Notification> handleGetAllNotifications() {
        User currentUser = this.handleGetCurrentUser();
        if(currentUser == null) return Collections.emptyList();

        return this.notificationRepository.findByRecipient_UserIdOrderByCreatedAtDesc(currentUser.getUserId());
    }

    @Override
    @Transactional
    public void handleMarkNotificationsAsSeen(List<Long> notificationIds) {
        User currentUser = this.handleGetCurrentUser();
        if (currentUser == null) return;

        List<Notification> notifications = this.notificationRepository
                .findByNotificationIdInAndRecipient_UserId(notificationIds, currentUser.getUserId());

        notifications.forEach(n -> n.setSeen(true));
        this.notificationRepository.saveAll(notifications);
    }

    @Override
    public void handleNotifyCommentBlog(Blog blog, Comment comment) {
        User actor = this.handleGetCurrentUser();
        if(actor == null) return;

        User recipient = blog.getAuthor();

        if (actor.getUserId() == recipient.getUserId()) return;

        Notification notification = new Notification();
        notification.setType(NotificationType.COMMENT);
        notification.setBlog(blog);
        notification.setComment(comment);
        notification.setActor(actor);
        notification.setRecipient(recipient);

        this.notificationRepository.save(notification);
        this.handlePushTo(notification);
    }

    @Override
    public void handleNotifyReplyComment(Comment parent, Comment reply) {
        User actor = this.handleGetCurrentUser();
        if(actor == null) return;

        User recipient = parent.getCommentedBy();

        if (actor.getUserId() == recipient.getUserId()) return;

        Notification notification = new Notification();
        notification.setType(NotificationType.REPLY);
        notification.setBlog(parent.getBlog());
        notification.setReply(reply);
        notification.setRepliedOnComment(parent);
        notification.setActor(actor);
        notification.setRecipient(recipient);

        this.notificationRepository.save(notification);
        this.handlePushTo(notification);
    }

    @Override
    public void handleNotifyLikeBlog(Blog blog) {
        User actor = this.handleGetCurrentUser();
        if(actor == null || blog == null || blog.getAuthor() == null) return;

        User recipient = blog.getAuthor();

        if (actor.getUserId() == recipient.getUserId()) return;

        Optional<Notification> optNotification = this.notificationRepository.findByTypeAndActorAndBlogAndRecipient(
                NotificationType.LIKE, actor, blog, recipient
        );

        if(optNotification.isPresent()) {
            this.notificationRepository.delete(optNotification.get());
            return;
        }

        Notification notification = new Notification();
        notification.setType(NotificationType.LIKE);
        notification.setBlog(blog);
        notification.setActor(actor);
        notification.setRecipient(recipient);

        this.notificationRepository.save(notification);
        this.handlePushTo(notification);
    }

    private NotificationResponse covertToNotificationResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();

        response.setNotificationId(notification.getNotificationId());
        response.setType(notification.getType());
        response.setSeen(notification.isSeen());

        NotificationResponse.BlogNotification blog = new NotificationResponse.BlogNotification(
                notification.getBlog().getBlogId(),
                notification.getBlog().getTitle()
        );
        response.setBlog(blog);

        NotificationResponse.UserNotification actor = new NotificationResponse.UserNotification(
                notification.getActor().getUserId(),
                notification.getActor().getFullName() == null ? "" : notification.getActor().getFullName(),
                notification.getActor().getUsername(), notification.getActor().getAvatar()
        );
        response.setActor(actor);

        NotificationResponse.UserNotification recipient = new NotificationResponse.UserNotification(
                notification.getRecipient().getUserId(),
                notification.getRecipient().getFullName() == null ? "" : notification.getRecipient().getFullName(),
                notification.getRecipient().getUsername(), notification.getRecipient().getAvatar()
        );
        response.setRecipient(recipient);

        if(notification.getComment() != null) {
            NotificationResponse.CommentNotification comment = new NotificationResponse.CommentNotification(
                    notification.getComment().getCommentId(),
                    notification.getComment().getComment()
            );
            response.setComment(comment);
        }

        if(notification.getReply() != null) {
            NotificationResponse.CommentNotification reply = new NotificationResponse.CommentNotification(
                    notification.getReply().getCommentId(),
                    notification.getReply().getComment()
            );
            response.setReply(reply);
        }

        if(notification.getRepliedOnComment() != null) {
            NotificationResponse.CommentNotification repliedOnComment = new NotificationResponse.CommentNotification(
                    notification.getRepliedOnComment().getCommentId(),
                    notification.getRepliedOnComment().getComment()
            );
            response.setRepliedOnComment(repliedOnComment);
        }

        return response;
    }
}
