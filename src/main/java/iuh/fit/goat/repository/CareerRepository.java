package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Career;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CareerRepository extends JpaRepository<Career, Long>, JpaSpecificationExecutor<Career> {
    boolean existsByName(String name);
}
