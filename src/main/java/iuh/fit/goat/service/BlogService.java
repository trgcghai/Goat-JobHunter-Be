package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.blog.BlogIdsRequest;
import iuh.fit.goat.dto.request.user.LikeBlogRequest;
import iuh.fit.goat.dto.response.blog.BlogResponse;
import iuh.fit.goat.dto.response.blog.BlogStatusResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Comment;
import iuh.fit.goat.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

public interface BlogService {
    Flux<ServerSentEvent<String>> stream(Long blogId);

    Blog handleCreateBlog(Blog blog);

    Blog handleUpdateBlog(Blog blog);

    void handleDeleteBlog(BlogIdsRequest request);

    Blog handleGetBlogById(long id);

    ResultPaginationResponse handleGetAllBlogs(Specification<Blog> spec, Pageable pageable);

    void handleIncrementTotalValue(Comment comment);

    List<Notification> handleLikeBlog(LikeBlogRequest likeBlogRequest);

    List<Object[]> handleGetAllTags(String keyword);

    List<BlogStatusResponse> handleAcceptBlogs(BlogIdsRequest request);

    List<BlogStatusResponse> handleRejectBlogs(BlogIdsRequest request);

    BlogResponse convertToBlogResponse(Blog blog);
}
