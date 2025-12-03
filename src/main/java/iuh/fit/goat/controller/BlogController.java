package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.request.blog.BlogCreateRequest;
import iuh.fit.goat.dto.request.blog.BlogIdsRequest;
import iuh.fit.goat.dto.request.blog.BlogUpdateRequest;
import iuh.fit.goat.dto.request.user.LikeBlogRequest;
import iuh.fit.goat.dto.response.blog.BlogResponse;
import iuh.fit.goat.dto.response.blog.BlogStatusResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Notification;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.BlogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/blogs")
@RequiredArgsConstructor
public class BlogController {
    private final BlogService blogService;

    @PostMapping
    public ResponseEntity<?> createBlog(@Valid @RequestBody BlogCreateRequest request) {
        Blog res = this.blogService.handleCreateBlog(request);
        BlogResponse blogResponse = this.blogService.convertToBlogResponse(res);
        return ResponseEntity.status(HttpStatus.CREATED).body(blogResponse);
    }

    @PutMapping
    public ResponseEntity<?> updateBlog(@Valid @RequestBody BlogUpdateRequest request) throws InvalidException {
        Blog res = this.blogService.handleUpdateBlog(request);
        if(res == null) throw new InvalidException("Blog doesn't exist");
        BlogResponse blogResponse = this.blogService.convertToBlogResponse(res);
        return ResponseEntity.status(HttpStatus.OK).body(blogResponse);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteBlog(@Valid @RequestBody BlogIdsRequest request) {
        this.blogService.handleDeleteBlog(request);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBlogById(@PathVariable("id") String id) throws InvalidException {
        Pattern pattern = Pattern.compile("^[0-9]+$");
        if(!pattern.matcher(id).matches()) throw new InvalidException("Id is number");

        Blog res = this.blogService.handleGetBlogById(Long.parseLong(id));
        if(res == null) throw new InvalidException("Blog doesn't exist");

        BlogResponse blogResponse = this.blogService.convertToBlogResponse(res);

        return ResponseEntity.status(HttpStatus.OK).body(blogResponse);
    }

    @GetMapping
    public ResponseEntity<ResultPaginationResponse> getAllBlogs(
            @Filter Specification<Blog> spec, Pageable pageable
    ) {
        ResultPaginationResponse res = this.blogService.handleGetAllBlogs(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @GetMapping("/available")
    public ResponseEntity<ResultPaginationResponse> getAllAvailableBlogs(
            @Filter Specification<Blog> spec, Pageable pageable
    ) {
        Specification<Blog> baseSpec = (spec != null) ? spec : Specification.unrestricted();

        Specification<Blog> finalSpec = baseSpec
                .and((root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("draft")))
                .and((root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("enabled")));

        ResultPaginationResponse res = this.blogService.handleGetAllBlogs(finalSpec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PutMapping("/liked-blogs")
    public ResponseEntity<List<Notification>> likeBlogs(@Valid @RequestBody LikeBlogRequest likeBlogRequest) {
        List<Notification> res = this.blogService.handleLikeBlog(likeBlogRequest);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @GetMapping("/tags")
    public ResponseEntity<List<Object[]>> getAllTags(String keyword) {
        List<Object[]> res = this.blogService.handleGetAllTags(keyword);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PutMapping("/enabled")
    public ResponseEntity<List<BlogStatusResponse>> acceptBlogs(
            @Valid @RequestBody BlogIdsRequest request
    ) {
        List<BlogStatusResponse> result = this.blogService.handleEnableBlogs(request);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PutMapping("/disabled")
    public ResponseEntity<List<BlogStatusResponse>> rejectBlogs(
            @Valid @RequestBody BlogIdsRequest request
    ) {
        List<BlogStatusResponse> result = this.blogService.handleDisableBlogs(request);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/me")
    public ResponseEntity<ResultPaginationResponse> getCurrentUserBlogs(
            @Filter Specification<Blog> spec,
            Pageable pageable
    ) throws InvalidException {
        ResultPaginationResponse res = this.blogService.handleGetBlogsByCurrentUser(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }
}