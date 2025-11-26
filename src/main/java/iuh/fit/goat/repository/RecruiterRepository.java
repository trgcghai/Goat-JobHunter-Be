package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecruiterRepository extends JpaRepository<Recruiter, Long>,
        JpaSpecificationExecutor<Recruiter> {
    List<Recruiter> findByUserIdIn(List<Long> userIds);

    Optional<Recruiter> findByContactEmail(String email);
}
