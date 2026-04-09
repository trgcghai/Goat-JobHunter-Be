package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.ActionType;
import iuh.fit.goat.common.Role;
import iuh.fit.goat.dto.request.blog.BlogCreateRequest;
import iuh.fit.goat.dto.request.blog.BlogIdsRequest;
import iuh.fit.goat.dto.request.blog.BlogUpdateRequest;
import iuh.fit.goat.dto.request.user.LikeBlogRequest;
import iuh.fit.goat.dto.response.StorageResponse;
import iuh.fit.goat.dto.response.blog.BlogResponse;
import iuh.fit.goat.dto.response.blog.BlogStatusResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.repository.BlogRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.*;
import iuh.fit.goat.util.EntityUtil;
import iuh.fit.goat.util.FileUploadUtil;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
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
    private final StorageService storageService;

    private final AccountRepository accountRepository;

    @Override
    public Blog handleCreateBlog(BlogCreateRequest request) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(email).orElse(null);

        // Handle file uploads
        List<String> imageUrls = new ArrayList<>();
        if (request.getFiles() != null) {
            for (MultipartFile file : request.getFiles()) {
                if (file.isEmpty()) continue;
                FileUploadUtil.assertAllowed(file);
                StorageResponse response = this.storageService.handleUploadFile(file, "blogs");
                imageUrls.add(response.getUrl());
            }
        }

        // Generate tags
        List<String> tags = this.aiService.generateBlogTags(request.getContent());

        Blog blog = new Blog();
        blog.setImages(imageUrls);
        blog.setContent(request.getContent());
        blog.setTags(tags);
        blog.setAuthor(currentAccount);
        blog.setEnabled(true);

        return this.blogRepository.save(blog);
    }

    @Override
    public Blog handleUpdateBlog(BlogUpdateRequest request) {
        Blog currentBlog = this.handleGetBlogById(request.getBlogId());

        if (currentBlog != null) {
            currentBlog.setImages(request.getImages());
            currentBlog.setContent(request.getContent());
            currentBlog.setTags(request.getTags());

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
        Account currentAccount = this.userService.handleGetAccountByEmail(currentEmail);
        if (currentAccount == null) return;

        if (!currentAccount.isEnabled() || !currentAccount.getRole().isActive()) return;

        this.blogRepository.deleteAllById(request.getBlogIds());

        if (currentAccount.getRole().getName().equalsIgnoreCase(Role.ADMIN.getValue())) {
            Map<String, List<Blog>> blogByEmail = blogs.stream()
                    .collect(Collectors.groupingBy(blog -> blog.getAuthor().getEmail()));

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
    public void handleIncrementTotalLikeValue(Long blogId, boolean liked) {
        int incrementVal = liked ? 1 : -1;
        Blog blog = this.handleGetBlogById(blogId);
        long newTotalLikes = blog.getActivity().getTotalLikes() + incrementVal;
        blog.getActivity().setTotalLikes(Math.max(newTotalLikes, 0));
        Blog updatedBlog = this.blogRepository.save(blog);

        if (liked) {
            this.notificationService.handleNotifyLikeBlog(updatedBlog);
        }
    }

    @Override
    public void handleIncrementTotalReadValue(Long blogId, String guestId) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get() : "";
        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);

        String viewer = currentAccount != null ? "User" + currentAccount.getAccountId() : "Guest" + guestId;
        String key = "view:blog:" + blogId + ":" + viewer;

        if (this.redisService.hasKey(key)) return;
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
    public List<Object[]> handleGetPopularTags(String keyword) {
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
                blogs.stream().collect(Collectors.groupingBy(blog -> blog.getAuthor().getEmail()));

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
        ).toList();
    }

    @Override
    @Transactional
    public List<BlogStatusResponse> handleDisableBlogs(BlogIdsRequest request) {
        List<Blog> blogs = this.blogRepository.findAllById(request.getBlogIds());
        if (blogs.isEmpty()) return Collections.emptyList();

        blogs.forEach(blog -> blog.setEnabled(false));
        this.blogRepository.saveAll(blogs);

        Map<String, List<Blog>> blogByEmail =
                blogs.stream().collect(Collectors.groupingBy(blog -> blog.getAuthor().getEmail()));

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
        ).toList();
    }

    @Override
    public ResultPaginationResponse handleGetBlogsByCurrentUser(Specification<Blog> spec, Pageable pageable) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("Account not authenticated"));

        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new InvalidException("Account not found"));

        Specification<Blog> authorSpec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("author"), currentAccount);

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
        response.setImages(blog.getImages());
        response.setContent(blog.getContent());
        response.setTags(blog.getTags());
        response.setEnabled(blog.isEnabled());
        response.setActivity(blog.getActivity());
        response.setCreatedAt(blog.getCreatedAt());
        response.setCreatedBy(blog.getCreatedBy());
        response.setUpdatedAt(blog.getUpdatedAt());
        response.setUpdatedBy(blog.getUpdatedBy());
        if (blog.getAuthor() != null) {
            BlogResponse.BlogAuthor author = getBlogAuthor(blog);
            response.setAuthor(author);
        } else {
            BlogResponse.BlogAuthor anonymous = new BlogResponse.BlogAuthor();
            anonymous.setFullName("Tác giả ẩn danh");
            response.setAuthor(anonymous);
        }
        return response;
    }

    private static BlogResponse.BlogAuthor getBlogAuthor(Blog blog) {
        Account realAccount = EntityUtil.unproxy(blog.getAuthor());
        String name = realAccount instanceof Company ? ((Company) realAccount).getName() : ((User) realAccount).getFullName();
        String avatar = realAccount instanceof Company ? ((Company) realAccount).getLogo() : ((User) realAccount).getAvatar();
        String bio = realAccount instanceof Company ? ((Company) realAccount).getDescription() : ((User) realAccount).getBio();
        String coverPhoto = realAccount instanceof Company ? ((Company) realAccount).getCoverPhoto() : ((User) realAccount).getCoverPhoto();
        String headline = realAccount instanceof Company ? ((Company) realAccount).getWebsite() : ((User) realAccount).getHeadline();

        return new BlogResponse.BlogAuthor(
                blog.getAuthor().getAccountId(),
                name,
                blog.getAuthor().getUsername(),
                avatar,
                bio,
                headline,
                coverPhoto,
                realAccount.getVisibility()
        );
    }
}