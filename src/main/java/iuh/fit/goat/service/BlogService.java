package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.LikeBlogRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Comment;
import iuh.fit.goat.entity.Notification;
import iuh.fit.goat.repository.CommentRepository;
import iuh.fit.goat.repository.NotificationRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface BlogService {

    NotificationRepository notificationRepository = null;
    CommentRepository commentRepository = null;

    Blog handleCreateBlog(Blog blog);

    Blog handleUpdateBlog(Blog blog);

    void handleDeleteBlog(long id);

    default void deleteRecursively(Comment comment) {
        if (comment.getChildren() != null) {
            for (Comment child : comment.getChildren()) {
                deleteRecursively(child);
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
    }

    Blog handleGetBlogById(long id);

    ResultPaginationResponse handleGetAllBlogs(Specification<Blog> spec, Pageable pageable);

    void handleIncrementTotalValue(Comment comment);

    List<Notification> handleLikeBlog(LikeBlogRequest likeBlogRequest);

    List<Object[]> handleGetAllTags(String keyword);
}
