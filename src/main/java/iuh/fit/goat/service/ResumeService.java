package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.resume.CreateResumeRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.resume.ResumeResponse;
import iuh.fit.goat.dto.response.resume.ResumeStatusResponse;
import iuh.fit.goat.entity.Resume;
import iuh.fit.goat.entity.ResumeEvaluation;
import iuh.fit.goat.exception.InvalidException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface ResumeService {
    Resume handleCreateResume(CreateResumeRequest request) throws InvalidException;

    void handleDeleteResume(Long resumeId);

    Resume handleGetResumeById(Long resumeId);

    ResultPaginationResponse handleGetAllResumes(Specification<Resume> spec, Pageable pageable);

    ResumeStatusResponse handleDefaultResume(Long resumeId) throws InvalidException;

    ResumeStatusResponse handleUnDefaultResume(Long resumeId) throws InvalidException;

    ResumeStatusResponse handlePublicResume(Long resumeId) throws InvalidException;

    ResumeStatusResponse handlePrivateResume(Long resumeId) throws InvalidException;

    Resume handleUpdateTitle(Long resumeId, String title) throws InvalidException;

    ResultPaginationResponse handleGetAllResumeEvaluationByResume(
            Specification<ResumeEvaluation> spec, Pageable pageable, Long resumeId
    ) throws InvalidException;

    ResumeResponse handleConvertToResumeResponse(Resume resume);
}
