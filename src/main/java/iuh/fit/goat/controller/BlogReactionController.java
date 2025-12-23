package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.blog.ReactionBlogRequest;
import iuh.fit.goat.dto.request.blog.UnreactBlogRequest;
import iuh.fit.goat.dto.response.blog.BlogReactionCheckResponse;
import iuh.fit.goat.dto.response.user.UserResponse;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.BlogReactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reactions")
@RequiredArgsConstructor
public class BlogReactionController {
    private final BlogReactionService blogReactionService;

    @PostMapping("/blogs")
    public ResponseEntity<UserResponse> reactToBlog(@Valid @RequestBody ReactionBlogRequest request)
            throws InvalidException {
        UserResponse userResponse = this.blogReactionService.handleReactToBlog(request);
        if (userResponse == null) {
            throw new InvalidException("Failed to react to blog");
        }
        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }

    @DeleteMapping("/blogs")
    public ResponseEntity<UserResponse> unreactToBlogs(@Valid @RequestBody UnreactBlogRequest request)
            throws InvalidException {
        UserResponse userResponse = this.blogReactionService.handleUnreactToBlogs(request.getBlogIds());
        if (userResponse == null) {
            throw new InvalidException("Failed to unreact to blogs");
        }
        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }

    @GetMapping("/blogs")
    public ResponseEntity<List<BlogReactionCheckResponse>> checkBlogReactions(@RequestParam List<Long> blogIds) {
        List<BlogReactionCheckResponse> result = this.blogReactionService.handleCheckBlogReactions(blogIds);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}