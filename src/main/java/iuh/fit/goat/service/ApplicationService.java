package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.ApplicationIdsRequest;
import iuh.fit.goat.dto.response.ApplicationResponse;
import iuh.fit.goat.dto.response.ApplicationStatusResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Application;
import iuh.fit.goat.entity.Job;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

public interface ApplicationService {
    ApplicationResponse handleCreateApplication(Application application);

    List<ApplicationStatusResponse> handleUpdateApplication(ApplicationIdsRequest request);

    void handleDeleteApplication(long id);

    Application handleGetApplicationById(long id);

    ResultPaginationResponse handleGetAllApplications(
            Specification<Application> spec, Pageable pageable
    );

    boolean checkApplicantAndJobExist(Application application);

    Applicant handleGetApplicant(Application application);

    Job handleGetJob(Application application);

    ApplicationResponse convertToApplicationResponse(Application application);
}
