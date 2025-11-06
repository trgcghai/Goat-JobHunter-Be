package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.NotificationType;
import iuh.fit.goat.dto.request.LikeBlogRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Comment;
import iuh.fit.goat.entity.Notification;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.repository.BlogRepository;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;

    @Override
    public Blog handleCreateBlog(Blog blog) {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        User currentUser = this.userRepository.findByContact_Email(email);
        blog.setAuthor(currentUser);
        return this.blogRepository.save(blog);
    }

    @Override
    public Blog handleUpdateBlog(Blog blog) {
        Blog currentBlog = this.handleGetBlogById(blog.getBlogId());

        if(currentBlog != null) {
            currentBlog.setTitle(blog.getTitle());
            currentBlog.setBanner(blog.getBanner());
            currentBlog.setDescription(blog.getDescription());
            currentBlog.setContent(blog.getContent());
            currentBlog.setTags(blog.getTags());
            currentBlog.setDraft(blog.isDraft());
            currentBlog.setEnabled(blog.isEnabled());
            currentBlog.setActivity(blog.getActivity());

            return this.blogRepository.save(currentBlog);
        }

        return null;
    }

    @Override
    public void handleDeleteBlog(long id) {
        Blog blog = this.handleGetBlogById(id);

        if(blog.getComments() != null) {
            List<Comment> comments = blog.getComments();
            comments.forEach(this::deleteRecursively);
        }
        if(blog.getNotifications() != null) {
            List<Notification> notifications = blog.getNotifications();
            this.notificationRepository.deleteAll(notifications);
        }

        this.blogRepository.delete(blog);
    }

    private void deleteRecursively(Comment comment) {
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

    @Override
    public Blog handleGetBlogById(long id) {
        return this.blogRepository.findById(id).orElse(null);
    }

    @Override
    public ResultPaginationResponse handleGetAllBlogs(Specification<Blog> spec, Pageable pageable) {
        Page<Blog> page = this.blogRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(page.getNumber() + 1);
        meta.setPageSize(page.getSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        return new ResultPaginationResponse(meta, page.getContent());
    }

    @Override
    public void handleIncrementTotalValue(Comment comment) {
        Blog blog = comment.getBlog();
        blog.getActivity().setTotalComments(blog.getActivity().getTotalComments() + 1);
        if(!comment.isReply()) {
            blog.getActivity().setTotalParentComments(blog.getActivity().getTotalParentComments() + 1);
        }
        this.blogRepository.save(blog);
    }

    @Override
    public List<Notification> handleLikeBlog(LikeBlogRequest likeBlogRequest) {
        int incrementVal = likeBlogRequest.isLiked() ? 1 : -1;
        Blog blog = this.handleGetBlogById(likeBlogRequest.getBlog().getBlogId());
        long newTotalLikes = blog.getActivity().getTotalLikes() + incrementVal;
        blog.getActivity().setTotalLikes(Math.max(newTotalLikes, 0));
        Blog updatedBlog = this.blogRepository.save(blog);

        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        User currentUser = this.userRepository.findByContact_Email(email);

        if(likeBlogRequest.isLiked()) {
            Notification notification = new Notification();
            notification.setType(NotificationType.LIKE);
            notification.setSeen(false);
            notification.setBlog(updatedBlog);
            notification.setActor(currentUser);
            notification.setRecipient(blog.getAuthor());

            this.notificationRepository.save(notification);
        } else {
            Optional<Notification> optNotification = this.notificationRepository.findByTypeAndActorAndBlogAndRecipient(
                    NotificationType.LIKE, currentUser, updatedBlog,  blog.getAuthor()
            );
            optNotification.ifPresent(this.notificationRepository :: delete);
        }

        return currentUser.getActorNotifications();
    }

    @Override
    public List<Object[]> handleGetAllTags(String keyword) {
        return this.blogRepository.findAllTags(keyword);
    }

}
