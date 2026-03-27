package iuh.fit.goat.repository;

import iuh.fit.goat.entity.BlogReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogReactionRepository extends JpaRepository<BlogReaction, Long> {
    Optional<BlogReaction> findByBlog_BlogIdAndAccount_AccountId(Long blogId, Long accountId);

    List<BlogReaction> findByBlog_BlogIdInAndAccount_AccountId(List<Long> blogIds, Long accountId);

    void deleteByBlog_BlogIdInAndAccount_AccountId(List<Long> blogIds, Long accountId);
}