package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.RecruiterUpdateRequest;
import iuh.fit.goat.dto.response.RecruiterResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Recruiter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface RecruiterService {
    Recruiter handleCreateRecruiter(Recruiter recruiter);

    void handleDeleteRecruiter(long id);

    Recruiter handleUpdateRecruiter(RecruiterUpdateRequest updateRequest);

    Recruiter handleGetRecruiterById(long id);

    ResultPaginationResponse handleGetAllRecruiters(Specification<Recruiter> spec, Pageable pageable);

    RecruiterResponse convertToRecruiterResponse(Recruiter recruiter);
}
