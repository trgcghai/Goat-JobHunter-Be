package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.blog.BlogCreateRequest;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.repository.BlogRepository;
import iuh.fit.goat.service.*;
import iuh.fit.goat.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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