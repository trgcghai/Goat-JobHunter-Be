package iuh.fit.goat.service;

import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Comment;
import iuh.fit.goat.entity.Notification;

import java.util.List;

public interface NotificationService {
    List<Notification> handleGetAllNotifications();

    void handleMarkNotificationsAsSeen(List<Long> notificationIds);

    void handleNotifyCommentBlog(Blog blog, Comment comment);

    void handleNotifyReplyComment(Comment parent, Comment reply);

    void handleNotifyLikeBlog(Blog blog);
}
