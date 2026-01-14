package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.application.ApplicationIdsRequest;
import iuh.fit.goat.dto.request.application.CreateApplicationRequest;
import iuh.fit.goat.dto.response.application.ApplicationResponse;
import iuh.fit.goat.dto.response.application.ApplicationStatusResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Application;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.exception.InvalidException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface ApplicationService {
    Application handleCreateApplication(CreateApplicationRequest request) throws InvalidException;

    List<Application> handleAcceptApplications(List<Long> applicationIds);

    List<ApplicationStatusResponse> handleRejectApplications(ApplicationIdsRequest request);
//
//    void handleDeleteApplication(long id);
//
//    Application handleGetApplicationById(long id);
//
//    ResultPaginationResponse handleGetAllApplications(
//            Specification<Application> spec, Pageable pageable
//    );
//
    boolean checkApplicantAndJobAndResumeExist(Long jobId, Long resumeId);

    boolean handleCanApplyToJob(Long jobId);

    Long handleCountApplicationsByApplicantForJob(Long jobId);

//    Applicant handleGetApplicant(Application application);
//
//    Job handleGetJob(Application application);

    ApplicationResponse handleConvertToApplicationResponse(Application application);
}
