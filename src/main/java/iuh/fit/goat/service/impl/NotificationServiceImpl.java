package iuh.fit.goat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.goat.enumeration.NotificationType;
import iuh.fit.goat.dto.response.notification.NotificationResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.BlogRepository;
import iuh.fit.goat.repository.CommentRepository;
import iuh.fit.goat.repository.NotificationRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.NotificationService;
import iuh.fit.goat.service.RedisService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
//    private final NotificationRepository notificationRepository;
//    private final UserRepository userRepository;
//    private final BlogRepository blogRepository;
//    private final CommentRepository commentRepository;
//    private final SimpMessagingTemplate messagingTemplate;
//    private final RedisService redisService;
//    private final ObjectMapper objectMapper;
//
//    private User handleGetCurrentUser() {
//        String currentEmail = SecurityUtil.getCurrentUserLogin()
//                .orElse("");
//        return this.userRepository.findByContact_Email(currentEmail);
//    }
//
//    @Override
//    public Notification createNotification(Notification notification) {
//        return this.notificationRepository.save(notification);
//    }
//
//    @Override
//    public List<Notification> handleGetAllNotifications() {
//        User currentUser = this.handleGetCurrentUser();
//        if (currentUser == null) return Collections.emptyList();
//
//        return this.notificationRepository
//                .findByRecipient_UserIdOrderByCreatedAtDesc(currentUser.getUserId());
//    }
//
//    @Override
//    @Transactional
//    public void handleMarkNotificationsAsSeen(List<Long> notificationIds) {
//        User currentUser = this.handleGetCurrentUser();
//        if (currentUser == null) return;
//
//        List<Notification> notifications = this.notificationRepository
//                .findByNotificationIdInAndRecipient_UserId(notificationIds, currentUser.getUserId());
//
//        notifications.forEach(n -> n.setSeen(true));
//        this.notificationRepository.saveAll(notifications);
//    }
//
//    @Override
//    public void handleNotifyCommentBlog(Blog blog, Comment comment) {
//        User actor = this.handleGetCurrentUser();
//        if (actor == null) return;
//
//        User recipient = blog.getAuthor();
//        if (actor.getUserId() == recipient.getUserId()) return;
//
//        String redisKey = String.format("notification:%d:blog:%d:recipient:%d",
//                NotificationType.COMMENT.ordinal(), blog.getBlogId(), recipient.getUserId());
//
//        try {
//            if (redisService.hasKey(redisKey)) {
//                String existingPayload = redisService.getValue(redisKey);
//                @SuppressWarnings("unchecked")
//                Map<String, Object> existingData = objectMapper.readValue(existingPayload, Map.class);
//
//                @SuppressWarnings("unchecked")
//                List<Number> actorIds = (List<Number>) existingData.get("actorIds");
//
//                // Convert to Set for uniqueness
//                List<Long> newActorIds = actorIds.stream()
//                        .map(Number::longValue)
//                        .collect(Collectors.toList());
//
//                newActorIds.add(actor.getUserId());
//
//                existingData.put("actorIds", newActorIds);
//                existingData.put("commentId", comment.getCommentId());
//
//                String updatedPayload = objectMapper.writeValueAsString(existingData);
//                redisService.updateValue(redisKey, updatedPayload);
//            } else {
//                Notification notification = new Notification();
//                notification.setType(NotificationType.COMMENT);
//                notification.setBlog(blog);
//                notification.setComment(comment);
//                notification.setActors(List.of(actor));
//                notification.setRecipient(recipient);
//
//                setNotificationToRedis(notification, redisKey);
//            }
//        } catch (JsonProcessingException e) {
//            log.error("Failed to process comment notification for blog {}: {}", blog.getBlogId(), e.getMessage());
//        }
//    }
//
//    @Override
//    public void handleNotifyReplyComment(Comment parent, Comment reply) {
//        User actor = this.handleGetCurrentUser();
//        if (actor == null) return;
//
//        User recipient = parent.getCommentedBy();
//        if (actor.getUserId() == recipient.getUserId()) return;
//
//        String redisKey = String.format("notification:%d:comment:%d:recipient:%d",
//                NotificationType.REPLY.ordinal(), parent.getCommentId(), recipient.getUserId());
//
//        try {
//            if (redisService.hasKey(redisKey)) {
//                String existingPayload = redisService.getValue(redisKey);
//                @SuppressWarnings("unchecked")
//                Map<String, Object> existingData = objectMapper.readValue(existingPayload, Map.class);
//
//                @SuppressWarnings("unchecked")
//                List<Number> actorIds = (List<Number>) existingData.get("actorIds");
//
//                // Convert to Set for uniqueness
//                List<Long> newActorIds = actorIds.stream()
//                        .map(Number::longValue)
//                        .collect(Collectors.toList());
//
//                newActorIds.add(actor.getUserId());
//
//                existingData.put("actorIds", newActorIds);
//                existingData.put("replyId", reply.getCommentId());
//
//                String updatedPayload = objectMapper.writeValueAsString(existingData);
//                redisService.updateValue(redisKey, updatedPayload);
//            } else {
//                Notification notification = new Notification();
//                notification.setType(NotificationType.REPLY);
//                notification.setBlog(parent.getBlog());
//                notification.setReply(reply);
//                notification.setRepliedOnComment(parent);
//                notification.setActors(List.of(actor));
//                notification.setRecipient(recipient);
//
//                setNotificationToRedis(notification, redisKey);
//            }
//        } catch (JsonProcessingException e) {
//            log.error("Failed to process reply notification for comment {}: {}", parent.getCommentId(), e.getMessage());
//        }
//    }
//
//    @Override
//    public void handleNotifyLikeBlog(Blog blog) {
//        User actor = this.handleGetCurrentUser();
//        if (actor == null || blog == null || blog.getAuthor() == null) return;
//
//        User recipient = blog.getAuthor();
//        if (actor.getUserId() == recipient.getUserId()) return;
//
//        String redisKey = String.format("notification:%d:blog:%d:recipient:%d",
//                NotificationType.LIKE.ordinal(), blog.getBlogId(), recipient.getUserId());
//
//        try {
//            // Check if notification exists in Redis
//            if (redisService.hasKey(redisKey)) {
//                // Get existing data, add actor to list
//                String existingPayload = redisService.getValue(redisKey);
//                @SuppressWarnings("unchecked")
//                Map<String, Object> existingData = objectMapper.readValue(existingPayload, Map.class);
//
//                @SuppressWarnings("unchecked")
//                List<Number> actorIds = (List<Number>) existingData.get("actorIds");
//                Long actorId = actor.getUserId();
//
//                if (!actorIds.contains(actorId)) {
//                    actorIds.add(actorId);
//                    existingData.put("actorIds", actorIds);
//
//                    String updatedPayload = objectMapper.writeValueAsString(existingData);
//                    redisService.updateValue(redisKey, updatedPayload);
//                }
//            } else {
//                // Create new notification in Redis
//                Notification notification = new Notification();
//                notification.setType(NotificationType.LIKE);
//                notification.setBlog(blog);
//                notification.setActors(List.of(actor));
//                notification.setRecipient(recipient);
//
//                setNotificationToRedis(notification, redisKey);
//            }
//        } catch (JsonProcessingException e) {
//            log.error("Failed to process like notification for blog {}: {}", blog.getBlogId(), e.getMessage());
//        }
//    }
//
//    @Override
//    public void handleNotifyFollowRecruiter(Recruiter recruiter) {
//        User actor = this.handleGetCurrentUser();
//        if (actor == null || recruiter == null) return;
//
//        if (actor.getUserId() == recruiter.getUserId()) return;
//
//        String redisKey = String.format("notification:%d:recipient:%d", NotificationType.FOLLOW.ordinal(), recruiter.getUserId());
//
//        try {
//            if (redisService.hasKey(redisKey)) {
//                String existingPayload = redisService.getValue(redisKey);
//                @SuppressWarnings("unchecked")
//                Map<String, Object> existingData = objectMapper.readValue(existingPayload, Map.class);
//
//                @SuppressWarnings("unchecked")
//                List<Number> actorIds = (List<Number>) existingData.get("actorIds");
//                Long actorId = actor.getUserId();
//
//                if (!actorIds.contains(actorId)) {
//                    actorIds.add(actorId);
//                    existingData.put("actorIds", actorIds);
//
//                    String updatedPayload = objectMapper.writeValueAsString(existingData);
//                    redisService.updateValue(redisKey, updatedPayload);
//                }
//
//                String updatedPayload = objectMapper.writeValueAsString(existingData);
//                redisService.updateValue(redisKey, updatedPayload);
//            } else {
//                Notification notification = new Notification();
//                notification.setType(NotificationType.FOLLOW);
//                notification.setActors(List.of(actor));
//                notification.setRecipient(recruiter);
//
//                setNotificationToRedis(notification, redisKey);
//            }
//        } catch (JsonProcessingException e) {
//            log.error("Failed to process follow notification for recruiter {}: {}", recruiter.getUserId(), e.getMessage());
//        }
//    }
//
//    @Override
//    public void sendNotificationToUser(User user, Notification notification) {
//
//        log.info("Sending notification to user {}: {}", user.getUserId(), notification.getNotificationId());
//
//        NotificationResponse response = convertToNotificationResponse(notification);
//        messagingTemplate.convertAndSendToUser(
//                user.getContact().getEmail(),
//                "/queue/notifications",
//                response
//        );
//    }
//
//    @Override
//    public NotificationResponse convertToNotificationResponse(Notification notification) {
//        NotificationResponse response = new NotificationResponse();
//
//        response.setNotificationId(notification.getNotificationId());
//        response.setType(notification.getType());
//        response.setSeen(notification.isSeen());
//        response.setCreatedAt(notification.getCreatedAt());
//
//        if (notification.getBlog() != null) {
//            NotificationResponse.BlogNotification blog = new NotificationResponse.BlogNotification(
//                    notification.getBlog().getBlogId(),
//                    notification.getBlog().getTitle()
//            );
//            response.setBlog(blog);
//        }
//
//        NotificationResponse.UserNotification actor = new NotificationResponse.UserNotification(
//                notification.getLastActor().getUserId(),
//                notification.getLastActor().getFullName() == null ? "" : notification.getLastActor().getFullName(),
//                notification.getLastActor().getUsername(),
//                notification.getLastActor().getAvatar()
//        );
//        response.setLastActor(actor);
//        response.setActorCount(notification.getActorCount());
//
//        NotificationResponse.UserNotification recipient = new NotificationResponse.UserNotification(
//                notification.getRecipient().getUserId(),
//                notification.getRecipient().getFullName() == null ? "" : notification.getRecipient().getFullName(),
//                notification.getRecipient().getUsername(),
//                notification.getRecipient().getAvatar()
//        );
//        response.setRecipient(recipient);
//
//        if (notification.getComment() != null) {
//            NotificationResponse.CommentNotification comment = new NotificationResponse.CommentNotification(
//                    notification.getComment().getCommentId(),
//                    notification.getComment().getComment()
//            );
//            response.setComment(comment);
//        }
//
//        if (notification.getReply() != null) {
//            NotificationResponse.CommentNotification reply = new NotificationResponse.CommentNotification(
//                    notification.getReply().getCommentId(),
//                    notification.getReply().getComment()
//            );
//            response.setReply(reply);
//        }
//
//        if (notification.getRepliedOnComment() != null) {
//            NotificationResponse.CommentNotification repliedOnComment = new NotificationResponse.CommentNotification(
//                    notification.getRepliedOnComment().getCommentId(),
//                    notification.getRepliedOnComment().getComment()
//            );
//            response.setRepliedOnComment(repliedOnComment);
//        }
//
//        return response;
//    }
//
//    @Override
//    public Notification buildNotification(Map<String, Object> data) {
//        Notification notification = new Notification();
//
//        notification.setType(NotificationType.valueOf((String) data.get("type")));
//        notification.setSeen(false);
//
//        Long recipientId = ((Number) data.get("recipientId")).longValue();
//        User recipient = userRepository.findById(recipientId)
//                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
//        notification.setRecipient(recipient);
//
//        @SuppressWarnings("unchecked")
//        List<Number> actorIds = (List<Number>) data.get("actorIds");
//        List<User> actors = actorIds.stream()
//                .map(id -> userRepository.findById(id.longValue()).orElse(null))
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//        notification.setActors(actors);
//
//        if (data.containsKey("blogId")) {
//            Long blogId = ((Number) data.get("blogId")).longValue();
//            Blog blog = blogRepository.findById(blogId).orElse(null);
//            notification.setBlog(blog);
//        }
//
//        if (data.containsKey("commentId")) {
//            Long commentId = ((Number) data.get("commentId")).longValue();
//            Comment comment = commentRepository.findById(commentId).orElse(null);
//            notification.setComment(comment);
//        }
//
//        if (data.containsKey("replyId")) {
//            Long replyId = ((Number) data.get("replyId")).longValue();
//            Comment reply = commentRepository.findById(replyId).orElse(null);
//            notification.setReply(reply);
//        }
//
//        if (data.containsKey("repliedOnCommentId")) {
//            Long repliedOnCommentId = ((Number) data.get("repliedOnCommentId")).longValue();
//            Comment repliedOnComment = commentRepository.findById(repliedOnCommentId).orElse(null);
//            notification.setRepliedOnComment(repliedOnComment);
//        }
//
//        return notification;
//    }
//
//    private void setNotificationToRedis(Notification notification, String redisKey) throws JsonProcessingException {
//        Map<String, Object> notificationData = new HashMap<>();
//
//        notificationData.put("type", notification.getType().name());
//        notificationData.put("recipientId", notification.getRecipient().getUserId());
//
//        List<Long> actorIds = notification.getActors().stream()
//                .map(User::getUserId)
//                .collect(Collectors.toList());
//        notificationData.put("actorIds", actorIds);
//
//        if (notification.getBlog() != null) {
//            notificationData.put("blogId", notification.getBlog().getBlogId());
//        }
//
//        if (notification.getComment() != null) {
//            notificationData.put("commentId", notification.getComment().getCommentId());
//        }
//
//        if (notification.getReply() != null) {
//            notificationData.put("replyId", notification.getReply().getCommentId());
//        }
//
//        if (notification.getRepliedOnComment() != null) {
//            notificationData.put("repliedOnCommentId", notification.getRepliedOnComment().getCommentId());
//        }
//
//        String payload = objectMapper.writeValueAsString(notificationData);
//        int throttleNotificationTime = 120; // 2 Minutes, 120 Seconds
//        int delay = 30; // 30 Seconds delay to ensure listener processes after throttle period
//
//        redisService.saveWithTTL(redisKey + ":listener", "1", throttleNotificationTime, TimeUnit.SECONDS);
//
//        // Save to Redis with TTL, listen event when key expires to create notification in DB
//        redisService.saveWithTTL(redisKey, payload, throttleNotificationTime + delay, TimeUnit.SECONDS);
//    }
}