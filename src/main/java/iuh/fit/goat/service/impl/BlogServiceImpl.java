package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.ActionType;
import iuh.fit.goat.enumeration.Role;
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
import iuh.fit.goat.service.*;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {
    // avoid circular dependency between user service impl, ai service impl and blog service impl
    @Lazy
    @Autowired
    private UserService userService;

    // avoid circular dependency between user service impl, ai service impl and blog service impl
    @Lazy
    @Autowired
    private AiService aiService;

    private final RedisService redisService;

    private final EmailNotificationService emailNotificationService;
    private final NotificationService notificationService;
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;


    @Override
    public Blog handleCreateBlog(BlogCreateRequest request) {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        User currentUser = this.userRepository.findByContact_Email(email);

        // Generate description if not provided
        String description = request.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = this.aiService.generateBlogDescription(request.getContent());
        }

        // Generate tags if not provided or empty
        List<String> tags = request.getTags();
        if (tags == null || tags.isEmpty()) {
            tags = this.aiService.generateBlogTags(request.getContent());
        }

        Blog blog = new Blog();
        blog.setTitle(request.getTitle());
        blog.setBanner(request.getBanner());
        blog.setDescription(description);
        blog.setContent(request.getContent());
        blog.setTags(tags);
        blog.setDraft(request.getDraft());
        blog.setAuthor(currentUser);
        blog.setEnabled(false);

        return this.blogRepository.save(blog);
    }

    @Override
    public Blog handleUpdateBlog(BlogUpdateRequest request) {
        Blog currentBlog = this.handleGetBlogById(request.getBlogId());

        if (currentBlog != null) {
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
        if (blogs.isEmpty()) return;

        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        User currentUser = this.userService.handleGetUserByEmail(currentEmail);
        if (currentUser == null) return;

        if (!currentUser.isEnabled() || !currentUser.getRole().isActive()) return;

        this.blogRepository.deleteAllById(request.getBlogIds());

        if (currentUser.getRole().getName().equalsIgnoreCase(Role.ADMIN.getValue())) {
            Map<String, List<Blog>> blogByEmail = blogs.stream()
                    .collect(Collectors.groupingBy(blog -> blog.getAuthor().getContact().getEmail()));

            blogByEmail.forEach((email, bs) -> {
                if (bs.isEmpty()) return;

                this.emailNotificationService.handleSendBlogActionNotice(
                        email, bs.getFirst().getAuthor().getUsername(),
                        bs, request.getReason(), ActionType.DELETE
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
                .map(this::convertToBlogResponse)
                .toList();

        return new ResultPaginationResponse(meta, blogResponses);
    }

    @Override
    public void handleIncrementTotalCommentValue(Comment comment) {
        Blog blog = comment.getBlog();
        blog.getActivity().setTotalComments(blog.getActivity().getTotalComments() + 1);
        if (!comment.isReply()) {
            blog.getActivity().setTotalParentComments(blog.getActivity().getTotalParentComments() + 1);
        }
        this.blogRepository.save(blog);
    }

    @Override
    public void handleIncrementTotalLikeValue(LikeBlogRequest likeBlogRequest) {
        int incrementVal = likeBlogRequest.isLiked() ? 1 : -1;
        Blog blog = this.handleGetBlogById(likeBlogRequest.getBlogId());
        long newTotalLikes = blog.getActivity().getTotalLikes() + incrementVal;
        blog.getActivity().setTotalLikes(Math.max(newTotalLikes, 0));
        Blog updatedBlog = this.blogRepository.save(blog);

        if(likeBlogRequest.isLiked()) {
            this.notificationService.handleNotifyLikeBlog(updatedBlog);
        }
    }

    @Override
    public void handleIncrementTotalReadValue(Long blogId, String guestId) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get(): "";
        User currentUser = this.userService.handleGetUserByEmail(currentEmail);

        String viewer = currentUser != null ? "User" + currentUser.getUserId() : "Guest" + guestId;
        String key = "view:blog:" + blogId + ":" + viewer;

        if(this.redisService.hasKey(key)) return;
        this.redisService.saveWithTTL(
                key,
                "1",
                60,
                TimeUnit.SECONDS
        );

        Blog blog = this.handleGetBlogById(blogId);
        blog.getActivity().setTotalReads(blog.getActivity().getTotalReads() + 1);
        this.blogRepository.save(blog);
    }

    @Override
    public List<Object[]> handleGetAllTags(String keyword) {
        return this.blogRepository.findAllTags(keyword);
    }

    @Override
    @Transactional
    public List<BlogStatusResponse> handleEnableBlogs(BlogIdsRequest request) {
        List<Blog> blogs = this.blogRepository.findAllById(request.getBlogIds());
        if (blogs.isEmpty()) return Collections.emptyList();

        blogs.forEach(blog -> blog.setEnabled(true));
        this.blogRepository.saveAll(blogs);

        Map<String, List<Blog>> blogByEmail =
                blogs.stream().collect(Collectors.groupingBy(blog -> blog.getAuthor().getContact().getEmail()));

        blogByEmail.forEach((email, bs) -> {
            if (bs.isEmpty()) return;

            this.emailNotificationService.handleSendBlogActionNotice(
                    email, bs.getFirst().getAuthor().getUsername(),
                    bs, null, ActionType.ACCEPT
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
    @Transactional
    public List<BlogStatusResponse> handleDisableBlogs(BlogIdsRequest request) {
        List<Blog> blogs = this.blogRepository.findAllById(request.getBlogIds());
        if (blogs.isEmpty()) return Collections.emptyList();

        blogs.forEach(blog -> blog.setEnabled(false));
        this.blogRepository.saveAll(blogs);

        Map<String, List<Blog>> blogByEmail =
                blogs.stream().collect(Collectors.groupingBy(blog -> blog.getAuthor().getContact().getEmail()));

        blogByEmail.forEach((email, bs) -> {
            if (bs.isEmpty()) return;

            this.emailNotificationService.handleSendBlogActionNotice(
                    email, bs.getFirst().getAuthor().getUsername(),
                    bs, request.getReason(), ActionType.REJECT
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