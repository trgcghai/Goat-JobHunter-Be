package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.resume.ResumeEvaluationResponse;
import iuh.fit.goat.entity.ResumeEvaluation;

public interface EvaluationService {
    ResumeEvaluation handleGetResumeEvaluationById(Long id);

    ResumeEvaluationResponse handleConvertToResumeEvaluationResponse(ResumeEvaluation resume);
}
