package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.request.CreateCommentRequest;
import iuh.fit.goat.dto.response.comment.CommentResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Comment;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.BlogService;
import iuh.fit.goat.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final BlogService blogService;

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@Valid @RequestBody CreateCommentRequest comment) throws InvalidException {
        Blog blog = this.blogService.handleGetBlogById(comment.getBlogId());
        if(blog == null) throw new InvalidException("Blog doesn't exist");

        Comment res = this.commentService.handleCreateComment(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                this.commentService.convertToCommentResponse(res)
        );
    }

    @PutMapping
    public ResponseEntity<CommentResponse> updateComment(@Valid @RequestBody Comment comment) throws InvalidException {
        Comment res = this.commentService.handleUpdateComment(comment);
        if(res == null) throw new InvalidException("Comment doesn't exist");
        return ResponseEntity.status(HttpStatus.CREATED).body(
                this.commentService.convertToCommentResponse(res)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable("id") String id) throws InvalidException {
        Pattern pattern = Pattern.compile("^\\d+$");
        if(!pattern.matcher(id).matches()) throw new InvalidException("Id is number");

        Comment res = this.commentService.handleGetCommentById(Long.parseLong(id));
        if(res == null) throw new InvalidException("Comment doesn't exist");

        this.commentService.handleDeleteComment(Long.parseLong(id));
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommentResponse> getCommentById(@PathVariable("id") String id) throws InvalidException {
        Pattern pattern = Pattern.compile("^\\d+$");
        if(!pattern.matcher(id).matches()) throw new InvalidException("Id is number");

        Comment res = this.commentService.handleGetCommentById(Long.parseLong(id));
        if(res == null) throw new InvalidException("Comment doesn't exist");

        return ResponseEntity.status(HttpStatus.OK).body(
                this.commentService.convertToCommentResponse(res)
        );
    }

    @GetMapping
    public ResponseEntity<ResultPaginationResponse> getAllComments(
            @Filter Specification<Comment> spec, Pageable pageable
    ) {
        ResultPaginationResponse res = this.commentService.handleGetAllComments(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @GetMapping("/blog/{id}")
    public ResponseEntity<List<CommentResponse>> getAllCommentsByBlog(
            @PathVariable("id") long id
    ) {
        List<CommentResponse> res = this.commentService.handleGetCommentsByBlogId(id);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }
}