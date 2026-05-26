package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.blog.ReactionBlogRequest;
import iuh.fit.goat.dto.response.account.UserResponse;
import iuh.fit.goat.dto.response.blog.BlogReactionCheckResponse;
import iuh.fit.goat.dto.response.company.CompanyResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.ReactionType;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.repository.BlogReactionRepository;
import iuh.fit.goat.repository.BlogRepository;
import iuh.fit.goat.service.BlogService;
import iuh.fit.goat.service.CompanyService;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private Company currentCompany;
    private Blog blog;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setAccountId(501L);
        currentUser.setEmail("reader@example.com");
        currentUser.setUsername("reader");
        currentUser.setFullName("Reader One");

        currentCompany = new Company();
        currentCompany.setAccountId(601L);
        currentCompany.setEmail("company@example.com");
        currentCompany.setUsername("company");
        currentCompany.setName("Goat Corp");
        currentCompany.setLogo("logo.png");

        blog = new Blog();
        blog.setBlogId(701L);
        blog.setAuthor(currentUser);
    }

    @Test
    void handleReactToBlog_shouldCreateNewReactionAndReturnUserResponse() {
        ReactionBlogRequest request = new ReactionBlogRequest(701L, ReactionType.LIKE);

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("reader@example.com"));
            when(accountRepository.findByEmailWithRole("reader@example.com")).thenReturn(Optional.of(currentUser));
            when(blogRepository.findByBlogIdAndDeletedAtIsNull(701L)).thenReturn(Optional.of(blog));
            when(blogReactionRepository.findByBlog_BlogIdAndAccount_AccountId(701L, 501L)).thenReturn(Optional.empty());
            when(blogReactionRepository.save(any(BlogReaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userService.convertToUserResponse(any(User.class))).thenReturn(new UserResponse());

            Object result = blogReactionService.handleReactToBlog(request);

            assertThat(result).isInstanceOf(UserResponse.class);
            verify(blogReactionRepository).save(any(BlogReaction.class));
            verify(blogService).handleIncrementTotalLikeValue(701L, true);
        }
    }

    @Test
    void handleReactToBlog_shouldUpdateExistingReactionType() {
        ReactionBlogRequest request = new ReactionBlogRequest(701L, ReactionType.LOVE);
        BlogReaction existing = new BlogReaction();
        existing.setReactionId(1L);
        existing.setBlog(blog);
        existing.setAccount(currentUser);
        existing.setType(ReactionType.LIKE);

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("reader@example.com"));
            when(accountRepository.findByEmailWithRole("reader@example.com")).thenReturn(Optional.of(currentUser));
            when(blogRepository.findByBlogIdAndDeletedAtIsNull(701L)).thenReturn(Optional.of(blog));
            when(blogReactionRepository.findByBlog_BlogIdAndAccount_AccountId(701L, 501L)).thenReturn(Optional.of(existing));
            when(blogReactionRepository.save(any(BlogReaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userService.convertToUserResponse(any(User.class))).thenReturn(new UserResponse());

            blogReactionService.handleReactToBlog(request);

            assertThat(existing.getType()).isEqualTo(ReactionType.LOVE);
            verify(blogReactionRepository).save(existing);
            verify(blogService).handleIncrementTotalLikeValue(701L, true);
        }
    }

    @Test
    void handleReactToBlog_shouldReturnNullWhenLoginMissing() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.empty());

            assertThat(blogReactionService.handleReactToBlog(new ReactionBlogRequest(701L, ReactionType.LIKE))).isNull();
        }
    }

    @Test
    void handleReactToBlog_shouldReturnNullWhenAccountMissing() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("reader@example.com"));
            when(accountRepository.findByEmailWithRole("reader@example.com")).thenReturn(Optional.empty());

            assertThat(blogReactionService.handleReactToBlog(new ReactionBlogRequest(701L, ReactionType.LIKE))).isNull();
        }
    }

    @Test
    void handleReactToBlog_shouldReturnNullWhenBlogMissing() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("reader@example.com"));
            when(accountRepository.findByEmailWithRole("reader@example.com")).thenReturn(Optional.of(currentUser));
            when(blogRepository.findByBlogIdAndDeletedAtIsNull(701L)).thenReturn(Optional.empty());

            assertThat(blogReactionService.handleReactToBlog(new ReactionBlogRequest(701L, ReactionType.LIKE))).isNull();
        }
    }

    @Test
    void handleReactToBlog_shouldReturnCompanyResponseForCompanyAccount() {
        ReactionBlogRequest request = new ReactionBlogRequest(701L, ReactionType.SUPPORT);

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("company@example.com"));
            when(accountRepository.findByEmailWithRole("company@example.com")).thenReturn(Optional.of(currentCompany));
            when(blogRepository.findByBlogIdAndDeletedAtIsNull(701L)).thenReturn(Optional.of(blog));
            when(blogReactionRepository.findByBlog_BlogIdAndAccount_AccountId(701L, 601L)).thenReturn(Optional.empty());
            when(blogReactionRepository.save(any(BlogReaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(companyService.convertToCompanyResponse(any(Company.class))).thenReturn(new CompanyResponse());

            Object result = blogReactionService.handleReactToBlog(request);

            assertThat(result).isInstanceOf(CompanyResponse.class);
            verify(companyService).convertToCompanyResponse(currentCompany);
        }
    }

    @Test
    void handleUnreactToBlogs_shouldDeleteReactionsAndDecrementLikes() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("reader@example.com"));
            when(accountRepository.findByEmailWithRole("reader@example.com")).thenReturn(Optional.of(currentUser));
            doNothing().when(blogReactionRepository).deleteByBlog_BlogIdInAndAccount_AccountId(List.of(701L, 702L), 501L);
            doNothing().when(blogService).handleIncrementTotalLikeValue(701L, false);
            doNothing().when(blogService).handleIncrementTotalLikeValue(702L, false);
            when(userService.convertToUserResponse(any(User.class))).thenReturn(new UserResponse());

            Object result = blogReactionService.handleUnreactToBlogs(List.of(701L, 702L));

            assertThat(result).isInstanceOf(UserResponse.class);
            verify(blogService).handleIncrementTotalLikeValue(701L, false);
            verify(blogService).handleIncrementTotalLikeValue(702L, false);
        }
    }

    @Test
    void handleUnreactToBlogs_shouldReturnNullWhenLoginMissing() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.empty());

            assertThat(blogReactionService.handleUnreactToBlogs(List.of(701L))).isNull();
        }
    }

    @Test
    void handleUnreactToBlogs_shouldReturnNullWhenAccountMissing() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("reader@example.com"));
            when(accountRepository.findByEmailWithRole("reader@example.com")).thenReturn(Optional.empty());

            assertThat(blogReactionService.handleUnreactToBlogs(List.of(701L))).isNull();
        }
    }

    @Test
    void handleCheckBlogReactions_shouldReturnEmptyListWhenLoginMissing() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.empty());

            assertThat(blogReactionService.handleCheckBlogReactions(List.of(701L))).isEmpty();
        }
    }

    @Test
    void handleCheckBlogReactions_shouldReturnNullWhenAccountMissing() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("reader@example.com"));
            when(accountRepository.findByEmailWithRole("reader@example.com")).thenReturn(Optional.empty());

            assertThat(blogReactionService.handleCheckBlogReactions(List.of(701L))).isNull();
        }
    }

    @Test
    void handleCheckBlogReactions_shouldMapReactionTypesForBlogIds() {
        BlogReaction reaction1 = new BlogReaction();
        reaction1.setBlog(blog);
        reaction1.setAccount(currentUser);
        reaction1.setType(ReactionType.LIKE);

        Blog blog2 = new Blog();
        blog2.setBlogId(702L);
        BlogReaction reaction2 = new BlogReaction();
        reaction2.setBlog(blog2);
        reaction2.setAccount(currentUser);
        reaction2.setType(ReactionType.LOVE);

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("reader@example.com"));
            when(accountRepository.findByEmailWithRole("reader@example.com")).thenReturn(Optional.of(currentUser));
            when(blogReactionRepository.findByBlog_BlogIdInAndAccount_AccountId(List.of(701L, 702L), 501L))
                    .thenReturn(List.of(reaction1, reaction2));

            List<BlogReactionCheckResponse> result = blogReactionService.handleCheckBlogReactions(List.of(701L, 702L));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getBlogId()).isEqualTo(701L);
            assertThat(result.get(0).getReactionType()).isEqualTo(ReactionType.LIKE);
            assertThat(result.get(1).getBlogId()).isEqualTo(702L);
            assertThat(result.get(1).getReactionType()).isEqualTo(ReactionType.LOVE);
        }
    }
}