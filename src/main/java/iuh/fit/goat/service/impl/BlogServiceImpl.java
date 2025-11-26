package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.BlogActionType;
import iuh.fit.goat.common.NotificationType;
import iuh.fit.goat.common.Role;
import iuh.fit.goat.dto.request.blog.BlogIdsRequest;
import iuh.fit.goat.dto.request.user.LikeBlogRequest;
import iuh.fit.goat.dto.response.blog.BlogResponse;
import iuh.fit.goat.dto.response.blog.BlogStatusResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Comment;
import iuh.fit.goat.entity.Notification;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.repository.BlogRepository;
import iuh.fit.goat.repository.NotificationRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.BlogService;
import iuh.fit.goat.service.EmailService;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {
    private final UserService userService;
    private final EmailService emailService;
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;
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
    public void handleDeleteBlog(BlogIdsRequest request) {
        List<Blog> blogs = this.blogRepository.findAllById(request.getBlogIds());
        if(blogs.isEmpty()) return;

        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        User currentUser = this.userService.handleGetUserByEmail(currentEmail);
        if(currentUser == null) return;

        if(!currentUser.isEnabled() || !currentUser.getRole().isActive()) return;

        this.blogRepository.deleteAllById(request.getBlogIds());

        if(currentUser.getRole().getName().equalsIgnoreCase(Role.ADMIN.getValue())) {
            Map<String, List<Blog>> blogByEmail = blogs.stream()
                    .collect(Collectors.groupingBy(blog -> blog.getAuthor().getContact().getEmail()));

            blogByEmail.forEach((email, bs) -> {
                if(bs.isEmpty()) return;

                this.emailService.handleSendBlogActionNotice(
                        email, bs.getFirst().getAuthor().getUsername(),
                        bs, request.getReason(), BlogActionType.DELETE
                );
            });
        }
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

        List<BlogResponse> blogResponses = page.getContent().stream()
                .map(this :: convertToBlogResponse)
                .toList();

        return new ResultPaginationResponse(meta, blogResponses);
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

    @Override
    @Transactional
    public List<BlogStatusResponse> handleAcceptBlogs(BlogIdsRequest request) {
        List<Blog> blogs = this.blogRepository.findAllById(request.getBlogIds());
        if(blogs.isEmpty()) return Collections.emptyList();

        blogs.forEach(blog -> blog.setEnabled(true));
        this.blogRepository.saveAll(blogs);

        Map<String, List<Blog>> blogByEmail =
                blogs.stream().collect(Collectors.groupingBy(blog -> blog.getAuthor().getContact().getEmail()));

        blogByEmail.forEach((email, bs) -> {
            if(bs.isEmpty()) return;

            this.emailService.handleSendBlogActionNotice(
                    email, bs.getFirst().getAuthor().getUsername(),
                    bs, null, BlogActionType.ACCEPT
            );
        });

        return blogs.stream().map(
                blog -> new BlogStatusResponse(
                        blog.getBlogId(),
                        blog.isEnabled()
                )
        ).collect(Collectors.toList());
    }

    @Override
    public List<BlogStatusResponse> handleRejectBlogs(BlogIdsRequest request) {
        List<Blog> blogs = this.blogRepository.findAllById(request.getBlogIds());
        if(blogs.isEmpty()) return Collections.emptyList();

        blogs.forEach(blog -> blog.setEnabled(false));
        this.blogRepository.saveAll(blogs);

        Map<String, List<Blog>> blogByEmail =
                blogs.stream().collect(Collectors.groupingBy(blog -> blog.getAuthor().getContact().getEmail()));

        blogByEmail.forEach((email, bs) -> {
            if(bs.isEmpty()) return;

            this.emailService.handleSendBlogActionNotice(
                    email, bs.getFirst().getAuthor().getUsername(),
                    bs, request.getReason(), BlogActionType.REJECT
            );
        });

        return blogs.stream().map(
                blog -> new BlogStatusResponse(
                        blog.getBlogId(),
                        blog.isEnabled()
                )
        ).collect(Collectors.toList());
    }

    @Override
    public BlogResponse convertToBlogResponse(Blog blog) {
        BlogResponse response = new BlogResponse();

        response.setBlogId(blog.getBlogId());
        response.setTitle(blog.getTitle());
        response.setBanner(blog.getBanner());
        response.setDescription(blog.getDescription());
        response.setContent(blog.getContent());
        response.setTags(blog.getTags());
        response.setDraft(blog.isDraft());
        response.setEnabled(blog.isEnabled());
        response.setActivity(blog.getActivity());
        response.setCreatedAt(blog.getCreatedAt());
        response.setCreatedBy(blog.getCreatedBy());
        response.setUpdatedAt(blog.getUpdatedAt());
        response.setUpdatedBy(blog.getUpdatedBy());

        BlogResponse.BlogAuthor author = new BlogResponse.BlogAuthor(
                blog.getAuthor().getUserId(),
                blog.getAuthor().getFullName()
        );
        response.setAuthor(author);

        return response;
    }

}
