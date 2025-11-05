package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.ApplicationResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Application;
import iuh.fit.goat.entity.Job;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface ApplicationService {
    ApplicationResponse handleCreateApplication(Application application);

    ApplicationResponse handleUpdateApplication(Application application);

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
