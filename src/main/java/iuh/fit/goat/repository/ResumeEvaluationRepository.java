package iuh.fit.goat.repository;

import iuh.fit.goat.entity.ResumeEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeEvaluationRepository extends JpaRepository<ResumeEvaluation, Long>, JpaSpecificationExecutor<ResumeEvaluation> {
    Optional<ResumeEvaluation> findByResumeEvaluationIdAndDeletedAtIsNull(Long resumeEvaluationId);
}
