package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.blog.ReactionBlogRequest;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.ReactionType;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.repository.BlogReactionRepository;
import iuh.fit.goat.repository.BlogRepository;
import iuh.fit.goat.service.BlogService;
import iuh.fit.goat.service.CompanyService;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.SecurityUtil;
import iuh.fit.goat.dto.response.account.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogReactionServiceImplTest {

    @Mock private UserService userService;
    @Mock private CompanyService companyService;
    @Mock private BlogService blogService;
    @Mock private BlogReactionRepository blogReactionRepository;
    @Mock private BlogRepository blogRepository;
    @Mock private AccountRepository accountRepository;

    @InjectMocks
    private BlogReactionServiceImpl blogReactionService;

    private User currentUser;
    private Blog blog;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setAccountId(501L);
        currentUser.setEmail("reader@example.com");
        currentUser.setUsername("reader");
        currentUser.setFullName("Reader One");

        blog = new Blog();
        blog.setBlogId(701L);
        blog.setAuthor(currentUser);
    }

    @Test
    void handleReactToBlog_shouldCreateNewReaction() {
        ReactionBlogRequest request = new ReactionBlogRequest(701L, ReactionType.LIKE);

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("reader@example.com"));
            when(accountRepository.findByEmailWithRole("reader@example.com")).thenReturn(Optional.of(currentUser));
            when(blogRepository.findByBlogIdAndDeletedAtIsNull(701L)).thenReturn(Optional.of(blog));
            when(blogReactionRepository.findByBlog_BlogIdAndAccount_AccountId(701L, 501L)).thenReturn(Optional.empty());
            when(blogReactionRepository.save(any(BlogReaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(blogService).handleIncrementTotalLikeValue(701L, true);
            when(userService.convertToUserResponse(any(User.class))).thenReturn(new UserResponse());

            Object result = blogReactionService.handleReactToBlog(request);

            assertThat(result).isNotNull();
            verify(blogReactionRepository).save(any(BlogReaction.class));
            verify(blogService).handleIncrementTotalLikeValue(701L, true);
        }
    }

    @Test
    void handleUnreactToBlogs_shouldDeleteReactionAndDecrementLikes() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("reader@example.com"));
            when(accountRepository.findByEmailWithRole("reader@example.com")).thenReturn(Optional.of(currentUser));
            doNothing().when(blogReactionRepository).deleteByBlog_BlogIdInAndAccount_AccountId(List.of(701L), 501L);
            doNothing().when(blogService).handleIncrementTotalLikeValue(701L, false);
            when(userService.convertToUserResponse(any(User.class))).thenReturn(new UserResponse());

            Object result = blogReactionService.handleUnreactToBlogs(List.of(701L));

            assertThat(result).isNotNull();
            verify(blogService).handleIncrementTotalLikeValue(701L, false);
        }
    }
}