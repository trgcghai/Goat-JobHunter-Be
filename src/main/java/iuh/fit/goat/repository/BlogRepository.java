package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long>, JpaSpecificationExecutor<Blog> {

    @Query("""
        select t as tag, count(t) as count
        from Blog b join b.tags t
        where (:keyword is null or lower(t) like lower(concat('%', :keyword, '%')))
        group by t
        order by count(t) desc
        limit 15
    """)
    List<Object[]> findAllTags(@Param("keyword") String keyword);

    List<Blog> findByBlogIdIn(List<Long> blogIds);

    @Query("SELECT b FROM Blog b LEFT JOIN FETCH b.author WHERE b.enabled = true")
    Page<Blog> findAllAvailableWithAuthor(Specification<Blog> spec, Pageable pageable);
}
