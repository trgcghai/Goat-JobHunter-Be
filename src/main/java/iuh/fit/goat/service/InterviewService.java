package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.interview.CreateInterviewRequest;
import iuh.fit.goat.dto.response.interview.InterviewResponse;
import iuh.fit.goat.entity.Interview;

import java.util.List;

public interface InterviewService {

    List<InterviewResponse> handleCreateInterviews(CreateInterviewRequest request);

    InterviewResponse handleConvertToInterviewResponse(Interview interview);
}
