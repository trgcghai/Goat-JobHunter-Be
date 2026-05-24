package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.CreateCommentRequest;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.entity.embeddable.BlogActivity;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.repository.CommentRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.BlogService;
import iuh.fit.goat.service.NotificationService;
import iuh.fit.goat.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock private BlogService blogService;
    @Mock private NotificationService notificationService;
    @Mock private CommentRepository commentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AccountRepository accountRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User currentUser;
    private Blog blog;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setAccountId(1001L);
        currentUser.setEmail("commenter@example.com");
        currentUser.setUsername("commenter");
        currentUser.setFullName("Commenter One");

        blog = new Blog();
        blog.setBlogId(1101L);
        blog.setActivity(new BlogActivity());
    }

    @Test
    void handleCreateComment_shouldCreateRootCommentAndNotifyBlogOwner() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setBlogId(1101L);
        request.setComment("Nice article");

        when(blogService.handleGetBlogById(1101L)).thenReturn(blog);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("commenter@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("commenter@example.com")).thenReturn(Optional.of(currentUser));

            Comment result = commentService.handleCreateComment(request);

            assertThat(result.getComment()).isEqualTo("Nice article");
            assertThat(result.isReply()).isFalse();
            verify(blogService).handleIncrementTotalCommentValue(result);
            verify(notificationService).handleNotifyCommentBlog(blog, result);
        }
    }

    @Test
    void handleDeleteComment_shouldDeleteAndUpdateBlogCounters() {
        Comment comment = new Comment();
        comment.setCommentId(1201L);
        comment.setBlog(blog);
        comment.setReply(false);
        blog.getActivity().setTotalComments(2L);
        blog.getActivity().setTotalParentComments(1L);

        when(commentRepository.findById(1201L)).thenReturn(Optional.of(comment));

        commentService.handleDeleteComment(1201L);

        verify(commentRepository).delete(comment);
        verify(blogService).handleUpdateBlogActivity(blog);
        assertThat(blog.getActivity().getTotalComments()).isEqualTo(1L);
        assertThat(blog.getActivity().getTotalParentComments()).isEqualTo(0L);
    }
}