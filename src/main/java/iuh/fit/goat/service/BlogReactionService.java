package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.blog.ReactionBlogRequest;
import iuh.fit.goat.dto.response.blog.BlogReactionCheckResponse;

import java.util.List;

public interface BlogReactionService {
    Object handleReactToBlog(ReactionBlogRequest request);

    Object handleUnreactToBlogs(List<Long> blogIds);

    List<BlogReactionCheckResponse> handleCheckBlogReactions(List<Long> blogIds);
}