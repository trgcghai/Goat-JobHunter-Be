package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.applicant.ApplicantUpdateRequest;
import iuh.fit.goat.dto.response.applicant.ApplicantResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Applicant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface ApplicantService {
    Applicant handleCreateApplicant(Applicant applicant);
//
//    void handleDeleteApplicant(long id);

    Applicant handleUpdateApplicant(ApplicantUpdateRequest updateRequest);

    Applicant handleGetApplicantById(long id);

//    Applicant handleGetCurrentApplicant();
//
//    ResultPaginationResponse handleGetAllApplicants(Specification<Applicant> spec, Pageable pageable);
//
    ApplicantResponse convertToApplicantResponse(Applicant applicant);
}
