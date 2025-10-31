package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlogRepository extends JpaRepository<Blog, Long>, JpaSpecificationExecutor<Blog> {

    @Query(value = """
        select  distinct t, count(t)
        from Blog b Join b.tags t
        where (:keyword is null or lower(t) like lower(concat('%', :keyword, '%')))
        group by t
        order by count(t) desc
        """)
    List<Object[]> findAllTags(@Param("keyword") String keyword);

}
