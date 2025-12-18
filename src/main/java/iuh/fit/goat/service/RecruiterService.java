package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.recruiter.RecruiterUpdateRequest;
import iuh.fit.goat.dto.response.recruiter.RecruiterResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Recruiter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface RecruiterService {
    Recruiter handleCreateRecruiter(Recruiter recruiter);
//
//    void handleDeleteRecruiter(long id);
//
//    Recruiter handleUpdateRecruiter(RecruiterUpdateRequest updateRequest);
//
//    Recruiter handleGetRecruiterById(long id);
//
//    Recruiter handleGetCurrentRecruiter();
//
//    ResultPaginationResponse handleGetAllRecruiters(Specification<Recruiter> spec, Pageable pageable);
//
    RecruiterResponse convertToRecruiterResponse(Recruiter recruiter);
}
