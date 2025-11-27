package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.comment.CommentResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

public interface CommentService {
    Flux<ServerSentEvent<String>> stream(Long blogId);

    Comment handleCreateComment(Comment comment);

    Comment handleUpdateComment(Comment comment);

    void handleDeleteComment(long id);

    Comment handleGetCommentById(long id);

    ResultPaginationResponse handleGetAllComments(Specification<Comment> spec, Pageable pageable);

    List<CommentResponse> handleGetCommentsByBlogId(long blogId);

    CommentResponse convertToCommentResponse(Comment comment);
}
