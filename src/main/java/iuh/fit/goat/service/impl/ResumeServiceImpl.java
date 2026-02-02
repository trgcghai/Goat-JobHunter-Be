package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.resume.CreateResumeRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.resume.ResumeResponse;
import iuh.fit.goat.dto.response.resume.ResumeStatusResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Resume;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.repository.ApplicantRepository;
import iuh.fit.goat.repository.ResumeRepository;
import iuh.fit.goat.service.ResumeService;
import iuh.fit.goat.service.StorageService;
import iuh.fit.goat.util.FileUploadUtil;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {
    private final StorageService storageService;

    private final ResumeRepository resumeRepository;
    private final ApplicantRepository applicantRepository;
    private final AccountRepository accountRepository;

    @Override
    public Resume handleCreateResume(CreateResumeRequest request) throws InvalidException {
        String currentEmail = SecurityUtil.getCurrentUserEmail();
        if(currentEmail.isEmpty()) throw new InvalidException("You should be logged in");

        Applicant applicant = this.applicantRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        if(applicant == null) throw new InvalidException("Applicant not found");

        FileUploadUtil.assertAllowed(request.getFileUrl());

        String fileUrl = SecurityUtil.uploadImage(request.getFileUrl(), "resumes", storageService);
        String fileName = FileUploadUtil.getFileName(applicant.getFullName());

        Resume resume = new Resume();
        resume.setTitle(applicant.getFullName());
        resume.setFileUrl(fileUrl);
        resume.setFileName(fileName);
        resume.setFileSize(request.getFileUrl().getSize());
        resume.setApplicant(applicant);

        return this.resumeRepository.save(resume);
    }

    @Override
    public void handleDeleteResume(Long resumeId) {
        Resume resume = this.handleGetResumeById(resumeId);
        if(resume != null) {
            resume.onDelete();
            this.resumeRepository.save(resume);
        }
    }

    @Override
    public Resume handleGetResumeById(Long resumeId) {
        return this.resumeRepository.findByResumeIdAndDeletedAtIsNull(resumeId).orElse(null);
    }

    @Override
    public ResultPaginationResponse handleGetAllResumes(Specification<Resume> spec, Pageable pageable) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();
        if (currentEmail.isEmpty()) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        if (currentAccount == null) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        Role currentRole = currentAccount.getRole();
        Specification<Resume> resumesSpec;
        switch (currentRole.getName()) {
            case "RECRUITER", "COMPANY" -> resumesSpec = (root, query, cb) ->
                    cb.and(
                            cb.isNull(root.get("deletedAt")),
                            cb.isTrue(root.get("applicant").get("availableStatus")),
                            cb.isTrue(root.get("isPublic"))
                    );

            case "APPLICANT" -> resumesSpec = (root, query, cb) -> {
                assert query != null;
                query.orderBy(
                        cb.desc(root.get("isDefault")),
                        cb.desc(root.get("updatedAt"))
                );

                return cb.and(
                        cb.isNull(root.get("deletedAt")),
                        cb.equal(root.get("applicant").get("accountId"), currentAccount.getAccountId())
                );
            };

            default -> resumesSpec = (root, query, cb) ->
                    cb.and(
                            cb.isNull(root.get("deletedAt"))
                    );
        }

        Specification<Resume> finalSpec = spec == null ? resumesSpec : spec.and(resumesSpec);
        Page<Resume> page = this.resumeRepository.findAll(finalSpec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<ResumeResponse> resumeResponses = page.getContent().stream()
                .map(this::handleConvertToResumeResponse)
                .toList();

        return new ResultPaginationResponse(meta, resumeResponses);
    }

    @Override
    @Transactional
    public ResumeStatusResponse handleDefaultResume(Long resumeId) throws InvalidException {
        Resume resume = this.validateResumeOwnership(resumeId);

        this.resumeRepository.unsetOtherDefaults(resume.getApplicant().getAccountId(), resumeId);
        resume.setDefault(true);
        Resume defaultResume = this.resumeRepository.save(resume);

        return new ResumeStatusResponse(
                defaultResume.getResumeId(),
                defaultResume.isDefault()
        );
    }

    @Override
    public ResumeStatusResponse handleUnDefaultResume(Long resumeId) throws InvalidException {
        Resume resume = this.validateResumeOwnership(resumeId);

        resume.setDefault(false);
        Resume defaultResume = this.resumeRepository.save(resume);

        return new ResumeStatusResponse(
                defaultResume.getResumeId(),
                defaultResume.isDefault()
        );
    }

    @Override
    public ResumeStatusResponse handlePublicResume(Long resumeId) throws InvalidException {
        Resume resume = this.validateResumeOwnership(resumeId);

        resume.setPublic(true);
        Resume publicResume = this.resumeRepository.save(resume);

        return new ResumeStatusResponse(
                publicResume.getResumeId(),
                publicResume.isPublic()
        );
    }

    @Override
    public ResumeStatusResponse handlePrivateResume(Long resumeId) throws InvalidException {
        Resume resume = this.validateResumeOwnership(resumeId);

        resume.setPublic(false);
        Resume privateResume = this.resumeRepository.save(resume);

        return new ResumeStatusResponse(
                privateResume.getResumeId(),
                privateResume.isPublic()
        );
    }

    @Override
    public Resume handleUpdateTitle(Long resumeId, String title) throws InvalidException {
        Resume resume = this.validateResumeOwnership(resumeId);
        resume.setTitle(title);
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

    private Resume validateResumeOwnership(Long resumeId) throws InvalidException {
        String currentEmail = SecurityUtil.getCurrentUserEmail();
        Applicant applicant = this.applicantRepository.findByEmailAndDeletedAtIsNull(currentEmail)
                .orElseThrow(() -> new InvalidException("Applicant not found"));

        Resume resume = this.handleGetResumeById(resumeId);
        if(resume == null) throw new InvalidException("Resume not found");

        if(resume.getApplicant().getAccountId() != applicant.getAccountId()) {
            throw new InvalidException("You are not allowed to set this resume");
        }

        return resume;
    }
}
