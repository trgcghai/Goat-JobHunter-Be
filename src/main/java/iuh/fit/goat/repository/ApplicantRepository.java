package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, Long>,
        JpaSpecificationExecutor<Applicant> {
    Optional<Applicant> findByContact_Email(String email);
}
