package iuh.fit.goat.service;

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
import iuh.fit.goat.exception.InvalidException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface BlogService {
    Blog handleCreateBlog(BlogCreateRequest request);

    Blog handleUpdateBlog(BlogUpdateRequest request);

    void handleUpdateBlogActivity(Blog blog);

    void handleDeleteBlog(BlogIdsRequest blogIds);

    Blog handleGetBlogById(long id);

    ResultPaginationResponse handleGetAllBlogs(Specification<Blog> spec, Pageable pageable);

    void handleIncrementTotalCommentValue(Comment comment);

    void handleIncrementTotalLikeValue(LikeBlogRequest likeBlogRequest);

    void handleIncrementTotalReadValue(Long blogId, String guestId);

    List<Object[]> handleGetAllTags(String keyword);

    List<BlogStatusResponse> handleEnableBlogs(BlogIdsRequest request);

    List<BlogStatusResponse> handleDisableBlogs(BlogIdsRequest request);

    ResultPaginationResponse handleGetBlogsByCurrentUser(Specification<Blog> spec, Pageable pageable) throws InvalidException;

    BlogResponse convertToBlogResponse(Blog blog);
}
