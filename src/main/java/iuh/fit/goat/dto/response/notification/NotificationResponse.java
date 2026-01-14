package iuh.fit.goat.dto.response.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.enumeration.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
    private long notificationId;
    private NotificationType type;
    private boolean seen;
    private BlogNotification blog;
    private int actorCount;
    private UserNotification lastActor;
    private UserNotification recipient;
    private CommentNotification comment;
    private CommentNotification reply;
    private CommentNotification repliedOnComment;
    private Instant createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlogNotification {
        private long blogId;
        private String content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserNotification {
        private long userId;
        private String fullName;
        private String username;
        private String avatar;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentNotification {
        private long commentId;
        private String comment;
    }

}
