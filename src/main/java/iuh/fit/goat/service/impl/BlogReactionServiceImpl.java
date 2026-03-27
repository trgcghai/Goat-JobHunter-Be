package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.blog.ReactionBlogRequest;
import iuh.fit.goat.dto.response.blog.BlogReactionCheckResponse;
import iuh.fit.goat.dto.response.user.UserResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.repository.BlogReactionRepository;
import iuh.fit.goat.repository.BlogRepository;
import iuh.fit.goat.service.BlogReactionService;
import iuh.fit.goat.service.CompanyService;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlogReactionServiceImpl implements BlogReactionService {
    private final UserService userService;
    private final CompanyService companyService;

    private final BlogReactionRepository blogReactionRepository;
    private final BlogRepository blogRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public Object handleReactToBlog(ReactionBlogRequest request) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().orElse("");
        if (currentEmail.isEmpty()) {
            return null;
        }

        Account currentAccount = this.accountRepository.findByEmailWithRole(currentEmail).orElse(null);
        if (currentAccount == null) {
            return null;
        }

        Optional<Blog> blogOpt = this.blogRepository.findById(request.getBlogId());
        if (blogOpt.isEmpty()) {
            return null;
        }

        Blog blog = blogOpt.get();

        // Check if reaction already exists
        Optional<BlogReaction> existingReaction = this.blogReactionRepository
                .findByBlog_BlogIdAndAccount_AccountId(blog.getBlogId(), currentAccount.getAccountId());

        if (existingReaction.isPresent()) {
            // Update existing reaction
            BlogReaction reaction = existingReaction.get();
            reaction.setType(request.getReactionType());
            this.blogReactionRepository.save(reaction);
        } else {
            // Create new reaction
            BlogReaction newReaction = new BlogReaction();
            newReaction.setBlog(blog);
            newReaction.setAccount(currentAccount);
            newReaction.setType(request.getReactionType());
            this.blogReactionRepository.save(newReaction);
        }

        return currentAccount instanceof Company
                ? this.companyService.convertToCompanyResponse((Company) currentAccount)
                : this.userService.convertToUserResponse((User) currentAccount);
    }

    @Override
    @Transactional
    public Object handleUnreactToBlogs(List<Long> blogIds) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().orElse("");
        if (currentEmail.isEmpty()) {
            return null;
        }

        Account currentAccount = this.accountRepository.findByEmailWithRole(currentEmail).orElse(null);
        if (currentAccount == null) {
            return null;
        }

        // Delete reactions for specified blogs
        this.blogReactionRepository.deleteByBlog_BlogIdInAndAccount_AccountId(blogIds, currentAccount.getAccountId());

        return currentAccount instanceof Company
                ? this.companyService.convertToCompanyResponse((Company) currentAccount)
                : this.userService.convertToUserResponse((User) currentAccount);
    }

    @Override
    public List<BlogReactionCheckResponse> handleCheckBlogReactions(List<Long> blogIds) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().orElse("");
        if (currentEmail.isEmpty()) {
            return new ArrayList<>();
        }

        Account currentAccount = this.accountRepository.findByEmailWithRole(currentEmail).orElse(null);
        if (currentAccount == null) {
            return null;
        }

        List<BlogReaction> reactions = this.blogReactionRepository
                .findByBlog_BlogIdInAndAccount_AccountId(blogIds, currentAccount.getAccountId());

        return blogIds.stream()
                .map(blogId -> {
                    BlogReactionCheckResponse response = new BlogReactionCheckResponse();
                    response.setBlogId(blogId);

                    reactions.stream()
                            .filter(r -> r.getBlog().getBlogId() == blogId)
                            .findFirst()
                            .ifPresent(r -> response.setReactionType(r.getType()));

                    return response;
                })
                .toList();
    }
}