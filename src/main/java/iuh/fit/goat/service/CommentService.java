package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Comment;
import iuh.fit.goat.entity.Notification;
import iuh.fit.goat.repository.CommentRepository;
import iuh.fit.goat.repository.NotificationRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface CommentService {
    Comment handleCreateComment(Comment comment);

    Comment handleUpdateComment(Comment comment);

    void handleDeleteComment(long id);

    Comment handleGetCommentById(long id);

    ResultPaginationResponse handleGetAllComments(Specification<Comment> spec, Pageable pageable);
}
