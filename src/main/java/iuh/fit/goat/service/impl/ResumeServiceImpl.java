package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.resume.CreateResumeRequest;
import iuh.fit.goat.dto.response.resume.ResumeResponse;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Resume;
import iuh.fit.goat.repository.ApplicantRepository;
import iuh.fit.goat.repository.ResumeRepository;
import iuh.fit.goat.service.ResumeService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {
    private final ResumeRepository resumeRepository;
    private final ApplicantRepository applicantRepository;

    @Override
    public Resume handleCreateResume(CreateResumeRequest request) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();
        Applicant applicant = this.applicantRepository.findByEmail(currentEmail).orElse(null);
        if(applicant == null) return null;

        Resume resume = new Resume();
        resume.setTitle(request.getTitle());
        resume.setFileUrl(request.getFileUrl());
        resume.setFileName(request.getFileName());
        resume.setFileSize(request.getFileSize());
        resume.setSummary(request.getSummary());
        resume.setApplicant(applicant);

        return this.resumeRepository.save(resume);
    }

    @Override
    public ResumeResponse handleConvertToResumeResponse(Resume resume) {
        ResumeResponse response = new ResumeResponse();
        response.setResumeId(resume.getResumeId());
        response.setTitle(resume.getTitle());
        response.setFileUrl(resume.getFileUrl());
        response.setFileName(resume.getFileName());
        response.setFileSize(resume.getFileSize());
        response.setSummary(resume.getSummary());
        response.setDefault(resume.isDefault());
        response.setPublic(resume.isPublic());
        response.setAiScore(resume.getAiScore());
        response.setAiAnalysis(resume.getAiAnalysis());
        response.setAiSuggestions(resume.getAiSuggestions());
        response.setAnalyzedAt(resume.getAnalyzedAt());

        ResumeResponse.ResumeApplicant applicantResponse = new ResumeResponse.ResumeApplicant(
                resume.getApplicant().getAccountId(),
                resume.getApplicant().getEmail()
        );
        response.setApplicant(applicantResponse);

        response.setCreatedAt(resume.getCreatedAt());
        response.setCreatedBy(resume.getCreatedBy());
        response.setUpdatedAt(resume.getUpdatedAt());
        response.setUpdatedBy(resume.getUpdatedBy());

        return response;
    }
}
