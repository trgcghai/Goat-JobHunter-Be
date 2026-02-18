package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.resume.ResumeEvaluationResponse;
import iuh.fit.goat.entity.ResumeEvaluation;
import iuh.fit.goat.repository.ResumeEvaluationRepository;
import iuh.fit.goat.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {
    private final ResumeEvaluationRepository resumeEvaluationRepository;

    @Override
    public ResumeEvaluation handleGetResumeEvaluationById(Long id) {
        return this.resumeEvaluationRepository.findByResumeEvaluationIdAndDeletedAtIsNull(id).orElse(null);
    }

    @Override
    public ResumeEvaluationResponse handleConvertToResumeEvaluationResponse(ResumeEvaluation resume) {
        ResumeEvaluationResponse response = new ResumeEvaluationResponse();
        response.setResumeEvaluationId(resume.getResumeEvaluationId());
        response.setScore(resume.getScore());
        response.setStrengths(resume.getStrengths());
        response.setWeaknesses(resume.getWeaknesses());
        response.setMissingSkills(resume.getMissingSkills());
        response.setSkills(resume.getSkills());
        response.setSuggestions(resume.getSuggestions());
        response.setAiModel(resume.getAiModel());
        response.setCreatedAt(resume.getCreatedAt());
        response.setUpdatedAt(resume.getUpdatedAt());
        response.setResume(new ResumeEvaluationResponse.Resume(resume.getResume().getResumeId()));

        return response;
    }
}
