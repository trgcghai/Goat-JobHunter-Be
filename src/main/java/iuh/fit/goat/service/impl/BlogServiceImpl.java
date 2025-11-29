package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.BlogActionType;
import iuh.fit.goat.common.Role;
import iuh.fit.goat.config.components.RealTimeEventHub;
import iuh.fit.goat.dto.request.blog.BlogCreateRequest;
import iuh.fit.goat.dto.request.blog.BlogIdsRequest;
import iuh.fit.goat.dto.request.blog.BlogUpdateRequest;
import iuh.fit.goat.dto.request.user.LikeBlogRequest;
import iuh.fit.goat.dto.response.blog.BlogResponse;
import iuh.fit.goat.dto.response.blog.BlogStatusResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Comment;
import iuh.fit.goat.entity.Notification;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.BlogRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.BlogService;
import iuh.fit.goat.service.EmailNotificationService;
import iuh.fit.goat.service.NotificationService;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {
    private final UserService userService;
    private final EmailNotificationService emailNotificationService;
    private final NotificationService notificationService;
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;
    private final RealTimeEventHub eventHub;

    @Override
    public Flux<ServerSentEvent<String>> stream(Long blogId) {
        Blog currentBlog = this.handleGetBlogById(blogId);
        if(currentBlog == null) return Flux.empty();

        return this.eventHub.stream(
                "blog", Blog.class,
                c -> Objects.equals(c.getBlogId(), currentBlog.getBlogId()),
                this::convertToBlogResponse
        );
    }

    @Override
    public Blog handleCreateBlog(BlogCreateRequest request) {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        User currentUser = this.userRepository.findByContact_Email(email);

        Blog blog = new Blog();
        blog.setTitle(request.getTitle());
        blog.setBanner(request.getBanner());
        blog.setDescription(request.getDescription());
        blog.setContent(request.getContent());
        blog.setTags(request.getTags());
        blog.setDraft(request.getDraft());
        blog.setAuthor(currentUser);
        blog.setEnabled(false); // wait for admin to enable

        return this.blogRepository.save(blog);
    }

    @Override
    public Blog handleUpdateBlog(BlogUpdateRequest request) {
        Blog currentBlog = this.handleGetBlogById(request.getBlogId());

        if(currentBlog != null) {
            currentBlog.setTitle(request.getTitle());
            currentBlog.setBanner(request.getBanner());
            currentBlog.setDescription(request.getDescription());
            currentBlog.setContent(request.getContent());
            currentBlog.setTags(request.getTags());
            currentBlog.setDraft(request.getDraft());

            return this.blogRepository.save(currentBlog);
        }

        return null;
    }

    @Override
    public void handleUpdateBlogActivity(Blog blog) {
        this.blogRepository.save(blog);
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

                this.emailNotificationService.handleSendBlogActionNotice(
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
        Blog blog = this.handleGetBlogById(likeBlogRequest.getBlogId());
        long newTotalLikes = blog.getActivity().getTotalLikes() + incrementVal;
        blog.getActivity().setTotalLikes(Math.max(newTotalLikes, 0));
        Blog updatedBlog = this.blogRepository.save(blog);

        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        User currentUser = this.userRepository.findByContact_Email(email);

        this.notificationService.handleNotifyLikeBlog(updatedBlog);

        this.eventHub.push("blog", updatedBlog);

        return currentUser.getRecipientNotifications();
    }

    @Override
    public List<Object[]> handleGetAllTags(String keyword) {
        return this.blogRepository.findAllTags(keyword);
    }

    @Override
    @Transactional
    public List<BlogStatusResponse> handleEnableBlogs(BlogIdsRequest request) {
        List<Blog> blogs = this.blogRepository.findAllById(request.getBlogIds());
        if(blogs.isEmpty()) return Collections.emptyList();

        blogs.forEach(blog -> blog.setEnabled(true));
        this.blogRepository.saveAll(blogs);

        Map<String, List<Blog>> blogByEmail =
                blogs.stream().collect(Collectors.groupingBy(blog -> blog.getAuthor().getContact().getEmail()));

        blogByEmail.forEach((email, bs) -> {
            if(bs.isEmpty()) return;

            this.emailNotificationService.handleSendBlogActionNotice(
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
    public List<BlogStatusResponse> handleDisableBlogs(BlogIdsRequest request) {
        List<Blog> blogs = this.blogRepository.findAllById(request.getBlogIds());
        if(blogs.isEmpty()) return Collections.emptyList();

        blogs.forEach(blog -> blog.setEnabled(false));
        this.blogRepository.saveAll(blogs);

        Map<String, List<Blog>> blogByEmail =
                blogs.stream().collect(Collectors.groupingBy(blog -> blog.getAuthor().getContact().getEmail()));

        blogByEmail.forEach((email, bs) -> {
            if(bs.isEmpty()) return;

            this.emailNotificationService.handleSendBlogActionNotice(
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
    public ResultPaginationResponse handleGetBlogsByCurrentUser(Specification<Blog> spec, Pageable pageable) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        User currentUser = this.userRepository.findByContact_Email(email);
        if (currentUser == null) {
            throw new InvalidException("User not found");
        }

        // Combine spec with author filter
        Specification<Blog> authorSpec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("author"), currentUser);

        Specification<Blog> combinedSpec = spec != null
                ? spec.and(authorSpec)
                : authorSpec;

        Page<Blog> page = this.blogRepository.findAll(combinedSpec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(page.getNumber() + 1);
        meta.setPageSize(page.getSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<BlogResponse> blogResponses = page.getContent().stream()
                .map(this::convertToBlogResponse)
                .toList();

        return new ResultPaginationResponse(meta, blogResponses);
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
