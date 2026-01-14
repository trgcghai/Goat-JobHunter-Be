package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.interview.CreateInterviewRequest;
import iuh.fit.goat.dto.request.interview.FeedbackInterviewRequest;
import iuh.fit.goat.dto.request.interview.InterviewIdsRequest;
import iuh.fit.goat.dto.response.interview.InterviewResponse;
import iuh.fit.goat.dto.response.interview.InterviewStatusResponse;
import iuh.fit.goat.entity.Interview;
import iuh.fit.goat.exception.InvalidException;

import java.util.List;

public interface InterviewService {

    List<InterviewResponse> handleCreateInterviews(CreateInterviewRequest request);

    List<InterviewStatusResponse> handleCompleteInterviews(InterviewIdsRequest request);

    List<InterviewStatusResponse> handleCancelInterviews(InterviewIdsRequest request);

    List<InterviewStatusResponse> handleRescheduleInterviews(InterviewIdsRequest request);

    Interview handleGetInterviewById(Long interviewId);

    Interview handleFeedbackInterview(FeedbackInterviewRequest request) throws InvalidException;

    InterviewResponse handleConvertToInterviewResponse(Interview interview);
}
