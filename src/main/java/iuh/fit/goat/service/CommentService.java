package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface CommentService {
    Comment handleCreateComment(Comment comment);

    Comment handleUpdateComment(Comment comment);

    void handleDeleteComment(long id);

    Comment handleGetCommentById(long id);

    ResultPaginationResponse handleGetAllComments(Specification<Comment> spec, Pageable pageable);
}
