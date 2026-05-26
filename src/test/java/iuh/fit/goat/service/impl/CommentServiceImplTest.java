package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.CreateCommentRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.comment.CommentResponse;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    void handleCreateComment_shouldCreateReplyCommentAndNotifyParent() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setBlogId(1101L);
        request.setComment("I agree");
        request.setReplyTo(2201L);

        Comment parent = new Comment();
        parent.setCommentId(2201L);
        parent.setBlog(blog);
        parent.setComment("Nice article");
        parent.setCommentedBy(currentUser);

        when(blogService.handleGetBlogById(1101L)).thenReturn(blog);
        when(commentRepository.findById(2201L)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("commenter@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("commenter@example.com")).thenReturn(Optional.of(currentUser));

            Comment result = commentService.handleCreateComment(request);

            assertThat(result.isReply()).isTrue();
            assertThat(result.getParent()).isSameAs(parent);
            verify(notificationService).handleNotifyReplyComment(parent, result);
        }
    }

    @Test
    void handleCreateComment_shouldAllowAnonymousUserWhenLoginMissing() {
        CreateCommentRequest request = new CreateCommentRequest(1101L, "Anonymous comment", null);

        when(blogService.handleGetBlogById(1101L)).thenReturn(blog);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.empty());
            when(accountRepository.findByEmailAndDeletedAtIsNull("")).thenReturn(Optional.empty());

            Comment result = commentService.handleCreateComment(request);

            assertThat(result.getCommentedBy()).isNull();
            verify(notificationService).handleNotifyCommentBlog(blog, result);
        }
    }

    @Test
    void handleCreateComment_shouldSetReplyFalseWhenParentDoesNotExist() {
        CreateCommentRequest request = new CreateCommentRequest(1101L, "Orphan reply", 9999L);

        when(blogService.handleGetBlogById(1101L)).thenReturn(blog);
        when(commentService.handleGetCommentById(9999L)).thenReturn(null);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("commenter@example.com"));
            when(accountRepository.findByEmailAndDeletedAtIsNull("commenter@example.com")).thenReturn(Optional.of(currentUser));

            Comment result = commentService.handleCreateComment(request);

            assertThat(result.isReply()).isFalse();
            assertThat(result.getParent()).isNull();
        }
    }

    @Test
    void handleUpdateComment_shouldUpdateExistingComment() {
        Comment current = new Comment();
        current.setCommentId(1301L);
        current.setComment("Old text");

        Comment update = new Comment();
        update.setCommentId(1301L);
        update.setComment("New text");

        when(commentRepository.findById(1301L)).thenReturn(Optional.of(current));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment result = commentService.handleUpdateComment(update);

        assertThat(result.getComment()).isEqualTo("New text");
    }

    @Test
    void handleUpdateComment_shouldReturnNullWhenMissing() {
        Comment update = new Comment();
        update.setCommentId(1302L);
        update.setComment("New text");

        when(commentRepository.findById(1302L)).thenReturn(Optional.empty());

        assertThat(commentService.handleUpdateComment(update)).isNull();
    }

    @Test
    void handleDeleteComment_shouldDeleteLeafCommentAndUpdateCounters() {
        Comment comment = new Comment();
        comment.setCommentId(1201L);
        comment.setBlog(blog);
        comment.setReply(false);
        blog.getActivity().setTotalComments(2L);
        blog.getActivity().setTotalParentComments(1L);

        when(commentRepository.findById(1201L)).thenReturn(Optional.of(comment));

        commentService.handleDeleteComment(1201L);

        verify(commentRepository).delete(comment);
        verify(commentRepository).flush();
        verify(blogService).handleUpdateBlogActivity(blog);
        assertThat(blog.getActivity().getTotalComments()).isEqualTo(1L);
        assertThat(blog.getActivity().getTotalParentComments()).isEqualTo(0L);
    }

    @Test
    void handleDeleteComment_shouldDeleteNestedCommentsRecursively() {
        Comment parent = new Comment();
        parent.setCommentId(1401L);
        parent.setBlog(blog);
        parent.setReply(false);

        Comment child = new Comment();
        child.setCommentId(1402L);
        child.setBlog(blog);
        child.setReply(true);
        child.setParent(parent);

        parent.setChildren(new ArrayList<>(List.of(child)));
        blog.getActivity().setTotalComments(2L);
        blog.getActivity().setTotalParentComments(1L);

        when(commentRepository.findById(1401L)).thenReturn(Optional.of(parent));

        commentService.handleDeleteComment(1401L);

        verify(commentRepository).delete(child);
        verify(commentRepository).delete(parent);
        verify(commentRepository, times(2)).flush();
        assertThat(blog.getActivity().getTotalComments()).isEqualTo(0L);
        assertThat(blog.getActivity().getTotalParentComments()).isEqualTo(0L);
    }

    @Test
    void handleGetCommentById_shouldReturnCommentOrNull() {
        Comment comment = new Comment();
        comment.setCommentId(1501L);

        when(commentRepository.findById(1501L)).thenReturn(Optional.of(comment));
        when(commentRepository.findById(1502L)).thenReturn(Optional.empty());

        assertThat(commentService.handleGetCommentById(1501L)).isSameAs(comment);
        assertThat(commentService.handleGetCommentById(1502L)).isNull();
    }

    @Test
    void handleGetAllComments_shouldReturnPaginatedResponses() {
        Comment comment = new Comment();
        comment.setCommentId(1601L);
        comment.setComment("Paginated");
        comment.setCreatedAt(Instant.parse("2026-05-26T00:00:00Z"));
        comment.setBlog(blog);
        comment.setCommentedBy(currentUser);

        Pageable pageable = PageRequest.of(0, 10);
        when(commentRepository.findAll((Specification<Comment>) any(), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(comment), pageable, 1));

        ResultPaginationResponse result = commentService.handleGetAllComments(null, pageable);

        assertThat(result.getMeta().getPage()).isEqualTo(1);
        assertThat(result.getMeta().getTotal()).isEqualTo(1L);
        assertThat(result.getResult()).isInstanceOf(List.class);
        assertThat((List<?>) result.getResult()).hasSize(1);
    }

    @Test
    void handleGetCommentsByBlogId_shouldMapBlogComments() {
        Comment comment = new Comment();
        comment.setCommentId(1701L);
        comment.setComment("Blog list comment");
        comment.setCreatedAt(Instant.parse("2026-05-26T00:00:00Z"));
        comment.setBlog(blog);
        comment.setCommentedBy(currentUser);
        blog.setComments(List.of(comment));

        when(blogService.handleGetBlogById(1101L)).thenReturn(blog);

        List<CommentResponse> responses = commentService.handleGetCommentsByBlogId(1101L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBlog().getBlogId()).isEqualTo(1101L);
        assertThat(responses.get(0).getCommentedBy().getUsername()).isEqualTo("commenter");
    }

    @Test
    void convertToCommentResponse_shouldMapUserAndParent() {
        Comment parent = new Comment();
        parent.setCommentId(1801L);
        parent.setComment("Parent text");
        parent.setCommentedBy(currentUser);

        Comment child = new Comment();
        child.setCommentId(1802L);
        child.setComment("Child text");
        child.setBlog(blog);
        child.setCommentedBy(currentUser);
        child.setParent(parent);
        child.setCreatedAt(Instant.parse("2026-05-26T00:00:00Z"));

        CommentResponse response = commentService.convertToCommentResponse(child);

        assertThat(response.getCommentId()).isEqualTo(1802L);
        assertThat(response.getBlog().getBlogId()).isEqualTo(1101L);
        assertThat(response.getCommentedBy().getFullName()).isEqualTo("Commenter One");
        assertThat(response.getParent().getCommentId()).isEqualTo(1801L);
    }

    @Test
    void convertToCommentResponse_shouldMapCompanyCommenter() {
        Company company = new Company();
        company.setAccountId(1901L);
        company.setUsername("company1");
        company.setName("Goat Corp");
        company.setLogo("logo.png");

        Comment comment = new Comment();
        comment.setCommentId(1902L);
        comment.setComment("Company comment");
        comment.setCommentedBy(company);

        CommentResponse response = commentService.convertToCommentResponse(comment);

        assertThat(response.getCommentedBy().getFullName()).isEqualTo("Goat Corp");
        assertThat(response.getCommentedBy().getAvatar()).isEqualTo("logo.png");
    }

    @Test
    void handleDeleteComment_shouldDoNothingWhenCommentMissing() {
        when(commentRepository.findById(99999L)).thenReturn(Optional.empty());

        commentService.handleDeleteComment(99999L);

        verifyNoInteractions(blogService);
        verify(commentRepository, never()).delete(any(Comment.class));
    }

}