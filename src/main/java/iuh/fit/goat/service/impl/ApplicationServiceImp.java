package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.Status;
import iuh.fit.goat.dto.request.ApplicationIdsRequest;
import iuh.fit.goat.dto.response.ApplicationResponse;
import iuh.fit.goat.dto.response.ApplicationStatusResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.service.ApplicationService;
import iuh.fit.goat.service.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImp implements ApplicationService {
    private final EmailService emailService;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ApplicantRepository applicantRepository;

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
    @Transactional
    public List<ApplicationStatusResponse> handleUpdateApplication(ApplicationIdsRequest request) {
        List<Application> applications = this.applicationRepository.findAllById(request.getApplicationIds());
        if (applications.isEmpty()) return Collections.emptyList();

        Status status = Status.fromValue(request.getStatus());

        applications.forEach(app -> app.setStatus(status));
        this.applicationRepository.saveAll(applications);

        Map<String, List<Application>> applicationsByEmail =
                applications.stream().collect(Collectors.groupingBy(Application::getEmail));

        applicationsByEmail.forEach((email, apps) -> {
            if(apps.isEmpty()) return;

            String username = apps.getFirst().getApplicant().getUsername();
            String formattedDate = request.getInterviewDate() != null
                    ? request.getInterviewDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "";
            String note = request.getNote() != null ? request.getNote() : null;

            if(status == Status.ACCEPTED && !apps.isEmpty()) {
                this.emailService.handelSendApplicationStatusEmail(
                        email, username, apps, status.getValue(),
                        request.getInterviewType(), formattedDate, request.getLocation(), note,
                        null
                );
            } else if(status == Status.REJECTED && !apps.isEmpty()) {
                this.emailService.handelSendApplicationStatusEmail(
                        email, username, apps, status.getValue(),
                        null, null, null, null,
                        request.getReason()
                );
            }
        });

        return applications.stream()
                .map(app -> new ApplicationStatusResponse(
                        app.getApplicationId(),
                        app.getStatus().getValue()
                ))
                .collect(Collectors.toList());
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
