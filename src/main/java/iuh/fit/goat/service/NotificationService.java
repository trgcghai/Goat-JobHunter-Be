package iuh.fit.goat.service;

import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Comment;
import iuh.fit.goat.entity.Notification;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

public interface NotificationService {
    Flux<ServerSentEvent<String>> stream();

    List<Notification> handleGetAllNotifications();

    void handleMarkNotificationsAsSeen(List<Long> notificationIds);

    void handleNotifyCommentBlog(Blog blog, Comment comment);

    void handleNotifyReplyComment(Comment parent, Comment reply);

    void handleNotifyLikeBlog(Blog blog);

}
