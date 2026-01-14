package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.recruiter.RecruiterUpdateRequest;
import iuh.fit.goat.dto.response.recruiter.RecruiterResponse;
import iuh.fit.goat.entity.Recruiter;

public interface RecruiterService {
    Recruiter handleCreateRecruiter(Recruiter recruiter);

    Recruiter handleUpdateRecruiter(RecruiterUpdateRequest updateRequest);

    Recruiter handleGetRecruiterById(long id);

    RecruiterResponse convertToRecruiterResponse(Recruiter recruiter);
}
