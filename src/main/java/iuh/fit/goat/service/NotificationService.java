package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.notification.NotificationResponse;
import iuh.fit.goat.entity.*;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    List<Notification> handleGetAllNotifications();

    Notification createNotification(Notification notification);

    void handleMarkNotificationsAsSeen(List<Long> notificationIds);

    void handleNotifyCommentBlog(Blog blog, Comment comment);

    void handleNotifyReplyComment(Comment parent, Comment reply);

    void handleNotifyLikeBlog(Blog blog);

    void handleNotifyFollowRecruiter(Recruiter recruiter);

    void handleNotifyUnfollowRecruiter(Recruiter recruiter);

    void sendNotificationToUser(User user, Notification notification);

    NotificationResponse convertToNotificationResponse(Notification notification);

    Notification buildNotification(Map<String, Object> data);
}
