package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Comment;
import iuh.fit.goat.entity.Notification;
import iuh.fit.goat.repository.CommentRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface CommentService {

    final CommentRepository commentRepository = null;

    Comment handleCreateComment(Comment comment);

    Comment handleUpdateComment(Comment comment);

    void handleDeleteComment(long id);

    default int deleteRecursively(Comment comment) {
        int count = 1;
        if (comment.getChildren() != null) {
            for (Comment child : comment.getChildren()) {
                count += deleteRecursively(child);
            }
        }

        if (comment.getCommentNotifications() != null) {
            List<Notification> notifications = comment.getCommentNotifications();
//            this.notificationRepository.deleteAll(notifications);
        }
        if (comment.getReplyNotifications() != null) {
            List<Notification> notifications = comment.getReplyNotifications();
//            this.notificationRepository.deleteAll(notifications);
        }
        if (comment.getRepliedOnCommentNotifications() != null) {
            List<Notification> notifications = comment.getRepliedOnCommentNotifications();
//            this.notificationRepository.deleteAll(notifications);
        }
        this.commentRepository.delete(comment);

        return count;
    }

    Comment handleGetCommentById(long id);

    ResultPaginationResponse handleGetAllComments(Specification<Comment> spec, Pageable pageable);
}
