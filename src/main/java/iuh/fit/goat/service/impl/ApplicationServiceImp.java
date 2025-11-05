package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.ApplicationResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.util.*;
import iuh.fit.goat.dto.*;

import java.util.List;
import java.util.Optional;

@Service
public class ApplicationServiceImp implements iuh.fit.goat.service.ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ApplicantRepository applicantRepository;

    public ApplicationServiceImp(ApplicationRepository applicationRepository, ApplicantRepository applicantRepository,
                                 JobRepository jobRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.applicantRepository = applicantRepository;
    }

    @Override
    public ApplicationResponse handleCreateApplication(Application application) {
        Application result = this.applicationRepository.save(application);

        ApplicationResponse applicationResponse = new ApplicationResponse();
        applicationResponse.setApplicationId(result.getApplicationId());
        applicationResponse.setCreatedAt(result.getCreatedAt());
        applicationResponse.setCreatedBy(result.getCreatedBy());

        return applicationResponse;
    }

    @Override
    public ApplicationResponse handleUpdateApplication(Application application) {
        Application resApplication = this.handleGetApplicationById(application.getApplicationId());
        resApplication.setStatus(application.getStatus());
        Application result = this.applicationRepository.save(resApplication);

        ApplicationResponse applicationResponse = new ApplicationResponse();
        applicationResponse.setApplicationId(result.getApplicationId());
        applicationResponse.setUpdatedAt(result.getUpdatedAt());
        applicationResponse.setUpdatedBy(result.getUpdatedBy());

        return applicationResponse;
    }

    @Override
    public void handleDeleteApplication(long id) {
        this.applicationRepository.deleteById(id);
    }

    @Override
    public Application handleGetApplicationById(long id) {
        Optional<Application> application = this.applicationRepository.findById(id);
        return application.orElse(null);
    }

    @Override
    public ResultPaginationResponse handleGetAllApplications(
            Specification<Application> spec, Pageable pageable
    ) {
        Page<Application> page = this.applicationRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<ApplicationResponse> applications = page.getContent().stream()
                .map(this :: convertToApplicationResponse)
                .toList();

        return new ResultPaginationResponse(meta, applications);
    }

    @Override
    public boolean checkApplicantAndJobExist(Application application) {
        if(application.getApplicant() == null || application.getJob() == null) {
            return false;
        }

        Optional<Applicant> applicant = this.applicantRepository.findById(application.getApplicant().getUserId());
        Optional<Job> job = this.jobRepository.findById(application.getJob().getJobId());

        return applicant.isPresent() && job.isPresent();
    }

    @Override
    public Applicant handleGetApplicant(Application application) {
        Optional<Applicant> applicant = this.applicantRepository.findById(application.getApplicant().getUserId());
        return applicant.orElse(null);
    }

    @Override
    public Job handleGetJob(Application application) {
        Optional<Job> job = this.jobRepository.findById(application.getJob().getJobId());
        return job.orElse(null);
    }

    @Override
    public ApplicationResponse convertToApplicationResponse(Application application) {
        ApplicationResponse applicationResponse = new ApplicationResponse();
        Applicant applicant = this.handleGetApplicant(application);
        Job job = this.handleGetJob(application);

        applicationResponse.setApplicationId(application.getApplicationId());
        applicationResponse.setEmail(application.getEmail());
        applicationResponse.setResumeUrl(application.getResumeUrl());
        applicationResponse.setRecruiterName(job.getRecruiter().getFullName());
        applicationResponse.setStatus(application.getStatus());
        applicationResponse.setCreatedAt(application.getCreatedAt());
        applicationResponse.setCreatedBy(application.getCreatedBy());
        applicationResponse.setUpdatedAt(application.getUpdatedAt());
        applicationResponse.setUpdatedBy(application.getUpdatedBy());

        ApplicationResponse.UserApplication user = new ApplicationResponse.UserApplication(
                applicant.getUserId(), applicant.getFullName()
        );
        applicationResponse.setUser(user);

        ApplicationResponse.JobApplication resJob = new ApplicationResponse.JobApplication(
                job.getJobId(), job.getTitle()
        );
        applicationResponse.setJob(resJob);

        return applicationResponse;
    }
}
