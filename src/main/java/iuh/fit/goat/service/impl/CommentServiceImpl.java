package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.CreateCommentRequest;
import iuh.fit.goat.dto.response.comment.CommentResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Comment;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.repository.CommentRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.BlogService;
import iuh.fit.goat.service.CommentService;
import iuh.fit.goat.service.NotificationService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final BlogService blogService;
    private final NotificationService notificationService;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Override
    public Comment handleCreateComment(CreateCommentRequest request) {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        User currentUser = this.userRepository.findByContact_Email(email);

        Blog blog = this.blogService.handleGetBlogById(request.getBlogId());

        Comment comment = new Comment();
        comment.setComment(request.getComment());
        comment.setCommentedBy(currentUser);
        comment.setBlog(blog);

        if(request.getReplyTo() != null) {
            Comment parentComment = this.handleGetCommentById(request.getReplyTo());
            if(parentComment != null) {
                comment.setParent(parentComment);
                comment.setReply(true);
            } else {
                comment.setReply(false);
            }
        } else {
            comment.setReply(false);
        }

        Comment newComment = this.commentRepository.save(comment);

        this.blogService.handleIncrementTotalCommentValue(newComment);

        if(request.getReplyTo() != null) {
            this.notificationService.handleNotifyReplyComment(newComment.getParent(), newComment);
        } else {
            this.notificationService.handleNotifyCommentBlog(newComment.getBlog(), newComment);
        }

        return newComment;
    }

    @Override
    public Comment handleUpdateComment(Comment comment) {
        Comment currentComment = this.handleGetCommentById(comment.getCommentId());
        if(currentComment == null) return null;

        currentComment.setComment(comment.getComment());
        return this.commentRepository.save(currentComment);
    }

    @Override
    public void handleDeleteComment(long id) {
        Comment comment = this.handleGetCommentById(id);
        Blog blog = comment.getBlog();
        int deletedCount = deleteRecursively(comment);

        blog.getActivity().setTotalComments(blog.getActivity().getTotalComments() - deletedCount);
        if(!comment.isReply()) {
            blog.getActivity().setTotalParentComments(blog.getActivity().getTotalParentComments() - 1);
        }

        this.commentRepository.delete(comment);
        this.blogService.handleUpdateBlogActivity(blog);
    }

    @Override
    public Comment handleGetCommentById(long id) {
        return this.commentRepository.findById(id).orElse(null);
    }

    @Override
    public ResultPaginationResponse handleGetAllComments(Specification<Comment> spec, Pageable pageable) {
        Page<Comment> page = this.commentRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<CommentResponse> responses = page.getContent().stream()
                .map(this::convertToCommentResponse)
                .toList();

        return new ResultPaginationResponse(meta, responses);
    }

    @Override
    public List<CommentResponse> handleGetCommentsByBlogId(long blogId) {
        List<Comment> res = this.blogService.handleGetBlogById(blogId).getComments();

        return res.stream()
                .map(this::convertToCommentResponse)
                .toList();
    }

    @Override
    public CommentResponse convertToCommentResponse(Comment comment) {
        CommentResponse commentResponse = new CommentResponse();

        commentResponse.setCommentId(comment.getCommentId());
        commentResponse.setComment(comment.getComment());
        commentResponse.setReply(comment.isReply());
        commentResponse.setCreatedAt(comment.getCreatedAt());

        if(comment.getBlog() != null) {
            CommentResponse.BlogComment blog = new CommentResponse.BlogComment(
                    comment.getBlog().getBlogId(),
                    comment.getBlog().getTitle()
            );
            commentResponse.setBlog(blog);
        }

        if(comment.getCommentedBy() != null) {
            CommentResponse.UserCommented commentedBy = new CommentResponse.UserCommented(
                    comment.getCommentedBy().getUserId(),
                    comment.getCommentedBy().getFullName(),
                    comment.getCommentedBy().getUsername(),
                    comment.getCommentedBy().getAvatar()
            );
            commentResponse.setCommentedBy(commentedBy);
        }

        if(comment.getParent() != null) {
            CommentResponse.ParentComment parent = new CommentResponse.ParentComment(
                    comment.getParent().getCommentId(),
                    comment.getParent().getComment(),
                    new CommentResponse.UserCommented(
                            comment.getParent().getCommentedBy().getUserId(),
                            comment.getParent().getCommentedBy().getFullName(),
                            comment.getParent().getCommentedBy().getUsername(),
                            comment.getParent().getCommentedBy().getAvatar()
                    )
            );
            commentResponse.setParent(parent);
        }

        return commentResponse;
    }

    private int deleteRecursively(Comment comment) {
        int count = 1;
        if (comment.getChildren() != null) {
            for (Comment child : comment.getChildren()) {
                count += deleteRecursively(child);
            }
        }
        return count;
    }
}