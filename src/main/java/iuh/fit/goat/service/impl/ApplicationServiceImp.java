package iuh.fit.goat.service.impl;

import iuh.fit.goat.component.redis.application.ApplicationEventProducer;
import iuh.fit.goat.dto.request.application.ApplicationIdsRequest;
import iuh.fit.goat.dto.request.application.CreateApplicationRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.application.ApplicationStatusResponse;
import iuh.fit.goat.dto.result.application.ApplicationCreatedEvent;
import iuh.fit.goat.dto.result.application.ApplicationStatusEvent;
import iuh.fit.goat.enumeration.Status;
import iuh.fit.goat.dto.response.application.ApplicationResponse;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.ApplicationService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImp implements ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ApplicantRepository applicantRepository;
    private final ResumeRepository resumeRepository;

    private final ApplicationEventProducer eventProducer;

    @Override
    public Application handleCreateApplication(CreateApplicationRequest request) throws InvalidException {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        Job job = this.jobRepository.findById(request.getJobId()).orElse(null);
        Applicant applicant = this.applicantRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        Resume resume = this.resumeRepository.findById(request.getResumeId()).orElse(null);
        if(applicant == null || resume == null || job == null) return null;

        Application application = new Application();
        application.setEmail(request.getEmail() != null ?  request.getEmail() : currentEmail);
        application.setCoverLetter(request.getCoverLetter());
        application.setStatus(Status.PENDING);
        application.setJob(job);
        application.setApplicant(applicant);
        application.setResume(resume);
        Application saved= this.applicationRepository.save(application);

        ApplicationCreatedEvent event = new ApplicationCreatedEvent(
                saved.getApplicationId(), applicant.getEmail(), applicant.getFullName(),
                job.getCompany().getEmail(), job.getCompany().getName(), job.getTitle()
        );
        this.eventProducer.publishApplicationCreated(event);

        return saved;
    }

    @Override
    @Transactional
    public List<Application> handleAcceptApplications(List<Long> applicationIds) {
        List<Application> applications = this.applicationRepository.findAllById(applicationIds);
        if (applications.isEmpty()) return Collections.emptyList();

        List<Application> pendingApplications = applications.stream()
                .filter(app -> app.getStatus().getValue().equalsIgnoreCase(Status.PENDING.getValue()))
                .toList();

        pendingApplications.forEach(app -> app.setStatus(Status.ACCEPTED));
        return this.applicationRepository.saveAll(pendingApplications);
    }

    @Override
    @Transactional
    public List<ApplicationStatusResponse> handleRejectApplications(ApplicationIdsRequest request) {
        List<Application> applications = this.applicationRepository.findAllById(request.getApplicationIds());
        if (applications.isEmpty()) return Collections.emptyList();

        List<Application> pendingApplications = applications.stream()
                .filter(app -> app.getStatus().getValue().equalsIgnoreCase(Status.PENDING.getValue()))
                .toList();

        pendingApplications.forEach(app -> app.setStatus(Status.REJECTED));
        this.applicationRepository.saveAll(pendingApplications);

        Map<String, List<Application>> applicationsByEmail =
                pendingApplications.stream().collect(Collectors.groupingBy(Application::getEmail));

        applicationsByEmail.forEach((email, apps) -> {
            if(apps.isEmpty()) return;

            String username = apps.getFirst().getApplicant().getUsername();
            List<ApplicationStatusEvent> applicationEvents = apps.stream()
                    .map(app -> new ApplicationStatusEvent(
                            app.getJob().getTitle(),
                            app.getJob().getCompany().getName()
                    ))
                    .toList();

            try {
                this.eventProducer.publishApplicationStatus(
                        email,
                        username,
                        applicationEvents,
                        request.getReason(),
                        Status.REJECTED.getValue()
                );
            } catch (InvalidException e) {
                throw new IllegalArgumentException(e);
            }
        });

        return pendingApplications.stream()
                .map(app -> new ApplicationStatusResponse(
                        app.getApplicationId(),
                        app.getStatus().getValue()
                ))
                .toList();
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
                .map(this :: handleConvertToApplicationResponse)
                .toList();

        return new ResultPaginationResponse(meta, applications);
    }

    @Override
    public boolean checkApplicantAndJobAndResumeExist(Long jobId, Long resumeId) {
        if(jobId == null || resumeId == null) return false;

        String currentEmail = SecurityUtil.getCurrentUserEmail();
        Applicant applicant = this.applicantRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        Job job = this.jobRepository.findById(jobId).orElse(null);
        Resume resume = this.resumeRepository.findById(resumeId).orElse(null);

        return applicant != null && job != null && resume != null;
    }

    @Override
    public boolean handleCanApplyToJob(Long jobId) {
        if (jobId == null) return false;

        return this.handleCountApplicationsByApplicantForJob(jobId) < 3;
    }

    @Override
    public Long handleCountApplicationsByApplicantForJob(Long jobId) {
        if (jobId == null) return 0L;

        String currentEmail = SecurityUtil.getCurrentUserEmail();

        return this.applicationRepository.countApplicationsByApplicantAndJob(currentEmail, jobId);
    }

    @Override
    public Applicant handleGetApplicant(Application application) {
        Optional<Applicant> applicant = this.applicantRepository.findById(application.getApplicant().getAccountId());
        return applicant.orElse(null);
    }

    @Override
    public Job handleGetJob(Application application) {
        Optional<Job> job = this.jobRepository.findById(application.getJob().getJobId());
        return job.orElse(null);
    }

    @Override
    public ApplicationResponse handleConvertToApplicationResponse(Application application) {
        ApplicationResponse applicationResponse = new ApplicationResponse();
        ApplicationResponse.ApplicationJob job = new ApplicationResponse.ApplicationJob(
                application.getJob().getJobId(),
                application.getJob().getTitle()
        );
        ApplicationResponse.ApplicationApplicant applicant = new ApplicationResponse.ApplicationApplicant(
                application.getApplicant().getAccountId(),
                application.getApplicant().getEmail(),
                application.getApplicant().getFullName()
        );
        ApplicationResponse.ApplicationResume resume = new ApplicationResponse.ApplicationResume(
                application.getResume().getResumeId(),
                application.getResume().getFileUrl()
        );
        ApplicationResponse.ApplicationInterview interview = new ApplicationResponse.ApplicationInterview(
                application.getInterview() != null ? application.getInterview().getInterviewId() : null,
                application.getInterview() != null ? application.getInterview().getScheduledAt() : null
        );

        applicationResponse.setApplicationId(application.getApplicationId());
        applicationResponse.setEmail(application.getEmail());
        applicationResponse.setCoverLetter(application.getCoverLetter());
        applicationResponse.setStatus(application.getStatus());

        applicationResponse.setJob(job);
        applicationResponse.setApplicant(applicant);
        applicationResponse.setResume(resume);
        applicationResponse.setInterview(interview);

        applicationResponse.setCreatedAt(application.getCreatedAt());
        applicationResponse.setCreatedBy(application.getCreatedBy());
        applicationResponse.setUpdatedAt(application.getUpdatedAt());
        applicationResponse.setUpdatedBy(application.getUpdatedBy());

        return applicationResponse;
    }
}
