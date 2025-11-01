package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.NotificationType;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Comment;
import iuh.fit.goat.entity.Notification;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.repository.CommentRepository;
import iuh.fit.goat.repository.NotificationRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.BlogService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements iuh.fit.goat.service.CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final BlogService blogService;

    @Override
    public Comment handleCreateComment(Comment comment) {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        User currentUser = this.userRepository.findByContact_Email(email);
        comment.setCommentedBy(currentUser);
        if(comment.getBlog() != null) {
            Blog blog = this.blogService.handleGetBlogById(comment.getBlog().getBlogId());
            if(blog != null) {
                comment.setBlog(blog);
            }
        }
        if(comment.getParent() != null) {
            Comment parentComment = this.handleGetCommentById(comment.getParent().getCommentId());
            if(parentComment != null) {
                comment.setParent(parentComment);
                comment.setReply(true);
            }
        } else {
            comment.setReply(false);
        }
        Comment newComment = this.commentRepository.save(comment);

        this.blogService.handleIncrementTotalValue(newComment);

        Notification notification = new Notification();
        notification.setSeen(false);
        notification.setBlog(newComment.getBlog());
//        notification.setActor(currentUser);
        notification.setRecipient(newComment.getBlog().getAuthor());
        if(newComment.getParent() != null) {
            notification.setType(NotificationType.REPLY);
            notification.setReply(newComment);
            notification.setRepliedOnComment(newComment.getParent());
        } else {
            notification.setType(NotificationType.COMMENT);
            notification.setComment(newComment);
        }
//        this.notificationRepository.save(notification);

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
        this.blogService.handleUpdateBlog(blog);
    }

    private int deleteRecursively(Comment comment) {
        int count = 1;
        if (comment.getChildren() != null) {
            for (Comment child : comment.getChildren()) {
                count += deleteRecursively(child);
            }
        }

        if(comment.getCommentNotifications() != null) {
            List<Notification> notifications = comment.getCommentNotifications();
            this.notificationRepository.deleteAll(notifications);
        }
        if(comment.getReplyNotifications() != null) {
            List<Notification> notifications = comment.getReplyNotifications();
            this.notificationRepository.deleteAll(notifications);
        }
        if(comment.getRepliedOnCommentNotifications() != null) {
            List<Notification> notifications = comment.getRepliedOnCommentNotifications();
            this.notificationRepository.deleteAll(notifications);
        }
        this.commentRepository.delete(comment);

        return count;
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

        return new ResultPaginationResponse(meta, page.getContent());
    }
}
