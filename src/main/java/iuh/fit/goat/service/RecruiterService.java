package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.recruiter.RecruiterUpdateRequest;
import iuh.fit.goat.dto.response.recruiter.RecruiterResponse;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.exception.InvalidException;

public interface RecruiterService {
    Recruiter handleCreateRecruiter(Recruiter recruiter) throws InvalidException;

    Recruiter handleUpdateRecruiter(RecruiterUpdateRequest updateRequest) throws InvalidException;

    Recruiter handleGetRecruiterById(long id);

    RecruiterResponse convertToRecruiterResponse(Recruiter recruiter);
}
