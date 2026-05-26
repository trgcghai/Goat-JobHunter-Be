package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.blog.BlogCreateRequest;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.repository.BlogRepository;
import iuh.fit.goat.service.*;
import iuh.fit.goat.util.FileUploadUtil;
import iuh.fit.goat.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogServiceImplTest {

    @Mock private UserService userService;
    @Mock private AiService aiService;
    @Mock private RedisService redisService;
    @Mock private EmailNotificationService emailNotificationService;
    @Mock private NotificationService notificationService;
    @Mock private BlogRepository blogRepository;
    @Mock private StorageService storageService;
    @Mock private AccountRepository accountRepository;

    @InjectMocks
    private BlogServiceImpl blogService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setRoleId(5L);
        role.setName("USER");

        currentUser = new User();
        currentUser.setAccountId(808L);
        currentUser.setEmail("blogger@example.com");
        currentUser.setUsername("blogger");
        currentUser.setPassword("hash");
        currentUser.setRole(role);

        ReflectionTestUtils.setField(blogService, "aiService", aiService);
        ReflectionTestUtils.setField(blogService, "userService", userService);
    }

    @Test
    void handleCreateBlog_shouldSaveBlogWithGeneratedTags() throws Exception {
        BlogCreateRequest request = BlogCreateRequest.builder()
                .content("Hello blog content")
                .files(null)
                .build();

        Blog saved = new Blog();
        saved.setBlogId(901L);
        saved.setAuthor(currentUser);
        saved.setContent(request.getContent());

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("blogger@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("blogger@example.com")).thenReturn(Optional.of(currentUser));
            when(aiService.generateBlogTags("Hello blog content")).thenReturn(List.of("tag1", "tag2"));
            when(blogRepository.save(any(Blog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Blog result = blogService.handleCreateBlog(request);

            assertThat(result.getAuthor()).isSameAs(currentUser);
            assertThat(result.getTags()).containsExactly("tag1", "tag2");
            assertThat(result.isEnabled()).isTrue();
            verify(blogRepository).save(any(Blog.class));
        }
    }

    @Test
    void handleCreateBlog_shouldSetImagesWhenFileUploadSucceeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "cover.png", "image/png", "img".getBytes());
        BlogCreateRequest request = BlogCreateRequest.builder()
                .content("Hello blog with image")
                .files(new MockMultipartFile[]{file})
                .build();

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("blogger@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("blogger@example.com")).thenReturn(Optional.of(currentUser));
            when(aiService.generateBlogTags("Hello blog with image")).thenReturn(List.of("tag1"));
                when(storageService.handleUploadFile(eq(file), eq("blogs")))
                    .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(new iuh.fit.goat.dto.response.StorageResponse("public-1", "url-1")));
            when(blogRepository.save(any(Blog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Blog result = blogService.handleCreateBlog(request);

            assertThat(result.getImages()).containsExactly("url-1");
        }
    }

    @Test
    void handleCreateBlog_shouldUploadMultipleFilesInOrder() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "a.png", "image/png", "a".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "b.png", "image/png", "b".getBytes());
        BlogCreateRequest request = BlogCreateRequest.builder()
                .content("Multi file blog")
                .files(new MockMultipartFile[]{file1, file2})
                .build();

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("blogger@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("blogger@example.com")).thenReturn(Optional.of(currentUser));
            when(aiService.generateBlogTags("Multi file blog")).thenReturn(List.of("tag1", "tag2"));
                when(storageService.handleUploadFile(eq(file1), eq("blogs")))
                    .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(new iuh.fit.goat.dto.response.StorageResponse("public-a", "url-a")));
                when(storageService.handleUploadFile(eq(file2), eq("blogs")))
                    .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(new iuh.fit.goat.dto.response.StorageResponse("public-b", "url-b")));
            when(blogRepository.save(any(Blog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Blog result = blogService.handleCreateBlog(request);

            assertThat(result.getImages()).containsExactly("url-a", "url-b");
        }
    }

    @Test
    void handleCreateBlog_shouldSkipEmptyFiles() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("files", "empty.png", "image/png", new byte[0]);
        MockMultipartFile valid = new MockMultipartFile("files", "valid.png", "image/png", "v".getBytes());
        BlogCreateRequest request = BlogCreateRequest.builder()
                .content("Skip empty files")
                .files(new MockMultipartFile[]{empty, valid})
                .build();

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("blogger@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("blogger@example.com")).thenReturn(Optional.of(currentUser));
            when(aiService.generateBlogTags("Skip empty files")).thenReturn(List.of("tag"));
                when(storageService.handleUploadFile(eq(valid), eq("blogs")))
                    .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(new iuh.fit.goat.dto.response.StorageResponse("public-valid", "url-valid")));
            when(blogRepository.save(any(Blog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Blog result = blogService.handleCreateBlog(request);

            assertThat(result.getImages()).containsExactly("url-valid");
            verify(storageService, times(1)).handleUploadFile(eq(valid), eq("blogs"));
        }
    }

    @Test
    void handleCreateBlog_shouldCallTagGeneratorWithContent() throws Exception {
        BlogCreateRequest request = BlogCreateRequest.builder()
                .content("Tag source content")
                .files(null)
                .build();

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("blogger@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("blogger@example.com")).thenReturn(Optional.of(currentUser));
            when(aiService.generateBlogTags("Tag source content")).thenReturn(List.of("t1"));
            when(blogRepository.save(any(Blog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            blogService.handleCreateBlog(request);

            verify(aiService).generateBlogTags("Tag source content");
        }
    }

    @Test
    void handleCreateBlog_shouldSetAuthorWhenAccountExists() throws Exception {
        BlogCreateRequest request = BlogCreateRequest.builder()
                .content("Author mapping")
                .files(null)
                .build();

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("blogger@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("blogger@example.com")).thenReturn(Optional.of(currentUser));
            when(aiService.generateBlogTags("Author mapping")).thenReturn(List.of("tag"));
            when(blogRepository.save(any(Blog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Blog result = blogService.handleCreateBlog(request);

            assertThat(result.getAuthor()).isSameAs(currentUser);
        }
    }

    @Test
    void handleCreateBlog_shouldAllowAnonymousWhenNoLogin() throws Exception {
        BlogCreateRequest request = BlogCreateRequest.builder()
                .content("Anonymous content")
                .files(null)
                .build();

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.empty());
            when(accountRepository.findByEmailAndDeletedAtIsNull("")).thenReturn(Optional.empty());
            when(aiService.generateBlogTags("Anonymous content")).thenReturn(List.of("tag"));
            when(blogRepository.save(any(Blog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Blog result = blogService.handleCreateBlog(request);

            assertThat(result.getAuthor()).isNull();
        }
    }

    @Test
    void handleCreateBlog_shouldAllowMissingAccountWhenLoginLookupFails() throws Exception {
        BlogCreateRequest request = BlogCreateRequest.builder()
                .content("Missing account")
                .files(null)
                .build();

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("unknown@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("unknown@example.com")).thenReturn(Optional.empty());
            when(aiService.generateBlogTags("Missing account")).thenReturn(List.of("tag"));
            when(blogRepository.save(any(Blog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Blog result = blogService.handleCreateBlog(request);

            assertThat(result.getAuthor()).isNull();
        }
    }

    @Test
    void handleCreateBlog_shouldSetEnabledTrue() throws Exception {
        BlogCreateRequest request = BlogCreateRequest.builder()
                .content("Enabled check")
                .files(null)
                .build();

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("blogger@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("blogger@example.com")).thenReturn(Optional.of(currentUser));
            when(aiService.generateBlogTags("Enabled check")).thenReturn(List.of("tag"));
            when(blogRepository.save(any(Blog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Blog result = blogService.handleCreateBlog(request);

            assertThat(result.isEnabled()).isTrue();
        }
    }

    @Test
    void handleCreateBlog_shouldPropagateInvalidFileException() throws Exception {
        MockMultipartFile invalid = new MockMultipartFile("files", "bad.exe", "application/octet-stream", "x".getBytes());
        BlogCreateRequest request = BlogCreateRequest.builder()
                .content("Invalid file")
                .files(new MockMultipartFile[]{invalid})
                .build();

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class);
             MockedStatic<FileUploadUtil> mockedFileUploadUtil = mockStatic(FileUploadUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("blogger@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("blogger@example.com")).thenReturn(Optional.of(currentUser));
            mockedFileUploadUtil.when(() -> FileUploadUtil.assertAllowed(invalid)).thenThrow(new InvalidException("Invalid file type"));

            assertThatThrownBy(() -> blogService.handleCreateBlog(request))
                    .isInstanceOf(InvalidException.class)
                    .hasMessageContaining("Invalid file type");
        }
    }

    @Test
    void handleCreateBlog_shouldNotCallUploadForEmptyFilesOnly() throws Exception {
        MockMultipartFile empty1 = new MockMultipartFile("files", "empty1.png", "image/png", new byte[0]);
        MockMultipartFile empty2 = new MockMultipartFile("files", "empty2.png", "image/png", new byte[0]);
        BlogCreateRequest request = BlogCreateRequest.builder()
                .content("Empty only")
                .files(new MockMultipartFile[]{empty1, empty2})
                .build();

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("blogger@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("blogger@example.com")).thenReturn(Optional.of(currentUser));
            when(aiService.generateBlogTags("Empty only")).thenReturn(List.of("tag"));
            when(blogRepository.save(any(Blog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Blog result = blogService.handleCreateBlog(request);

            assertThat(result.getImages()).isEmpty();
            verify(storageService, never()).handleUploadFile(any(), any());
        }
    }

    @Test
    void handleCreateBlog_shouldCreateDistinctBlogInstance() throws Exception {
        BlogCreateRequest request = BlogCreateRequest.builder()
                .content("Instance check")
                .files(null)
                .build();

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("blogger@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("blogger@example.com")).thenReturn(Optional.of(currentUser));
            when(aiService.generateBlogTags("Instance check")).thenReturn(List.of("tag"));
            when(blogRepository.save(any(Blog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Blog result = blogService.handleCreateBlog(request);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("Instance check");
        }
    }

    @Test
    void handleIncrementTotalLikeValue_shouldIncreaseAndNotify() {
        Blog blog = new Blog();
        blog.setBlogId(902L);
        blog.setAuthor(currentUser);
        blog.getActivity().setTotalLikes(1L);

        when(blogRepository.findById(902L)).thenReturn(Optional.of(blog));
        when(blogRepository.save(any(Blog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        blogService.handleIncrementTotalLikeValue(902L, true);

        assertThat(blog.getActivity().getTotalLikes()).isEqualTo(2L);
        verify(notificationService).handleNotifyLikeBlog(blog);
    }
}