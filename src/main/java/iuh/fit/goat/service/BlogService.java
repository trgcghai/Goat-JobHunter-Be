package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.LikeBlogRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Comment;
import iuh.fit.goat.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface BlogService {
    Blog handleCreateBlog(Blog blog);

    Blog handleUpdateBlog(Blog blog);

    void handleDeleteBlog(long id);

    Blog handleGetBlogById(long id);

    ResultPaginationResponse handleGetAllBlogs(Specification<Blog> spec, Pageable pageable);

    void handleIncrementTotalValue(Comment comment);

    List<Notification> handleLikeBlog(LikeBlogRequest likeBlogRequest);

    List<Object[]> handleGetAllTags(String keyword);
}
