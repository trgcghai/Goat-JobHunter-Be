package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.blog.ReactionBlogRequest;
import iuh.fit.goat.dto.response.blog.BlogReactionCheckResponse;
import iuh.fit.goat.dto.response.user.UserResponse;

import java.util.List;

public interface BlogReactionService {
    UserResponse handleReactToBlog(ReactionBlogRequest request);

    UserResponse handleUnreactToBlogs(List<Long> blogIds);

    List<BlogReactionCheckResponse> handleCheckBlogReactions(List<Long> blogIds);
}