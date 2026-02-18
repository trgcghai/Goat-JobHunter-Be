package iuh.fit.goat.repository;

import iuh.fit.goat.entity.ResumeEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ResumeEvaluationRepository extends JpaRepository<ResumeEvaluation, Long>, JpaSpecificationExecutor<ResumeEvaluation> {
}
