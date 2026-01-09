package iuh.fit.goat.service.impl;

import iuh.fit.goat.component.redis.interview.InterviewEventProducer;
import iuh.fit.goat.dto.request.interview.CreateInterviewRequest;
import iuh.fit.goat.dto.response.interview.InterviewResponse;
import iuh.fit.goat.entity.Application;
import iuh.fit.goat.entity.Interview;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.enumeration.InterviewStatus;
import iuh.fit.goat.repository.InterviewRepository;
import iuh.fit.goat.repository.RecruiterRepository;
import iuh.fit.goat.service.ApplicationService;
import iuh.fit.goat.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {
    private final ApplicationService applicationService;

    private final InterviewRepository interviewRepository;
    private final RecruiterRepository recruiterRepository;

    private final InterviewEventProducer eventProducer;

    @Override
    public List<InterviewResponse> handleCreateInterviews(CreateInterviewRequest request) {
        Recruiter recruiter = this.recruiterRepository.findById(request.getInterviewerId()).orElse(null);
        if(recruiter == null) return Collections.emptyList();

        List<Application> applications = this.applicationService.handleAcceptApplications(request.getApplicationIds());
        if(applications.isEmpty()) return Collections.emptyList();

        List<Interview> interviews = new ArrayList<>(applications.size());
        for(Application application : applications) {
            Interview interview = Interview.builder()
                    .scheduledAt(request.getScheduledAt())
                    .durationMinutes(request.getDurationMinutes())
                    .type(request.getType())
                    .status(InterviewStatus.SCHEDULED)
                    .location(request.getLocation())
                    .notes(request.getNotes())
                    .interviewer(recruiter)
                    .application(application)
                    .build();

            if(request.getMeetingLink() != null) interview.setMeetingLink(request.getMeetingLink());

            Interview saved = this.interviewRepository.save(interview);
            interviews.add(saved);
        }

        List<InterviewResponse> responses = interviews.stream()
                .map(this::handleConvertToInterviewResponse)
                .toList();

        responses.forEach(this.eventProducer::publishInterviewCreated);

        return responses;
    }

    @Override
    public InterviewResponse handleConvertToInterviewResponse(Interview interview) {
        InterviewResponse response = new InterviewResponse();
        InterviewResponse.InterviewUser interviewer = new InterviewResponse.InterviewUser(
                interview.getInterviewer().getAccountId(),
                interview.getInterviewer().getFullName(),
                interview.getInterviewer().getEmail()
        );
        InterviewResponse.InterviewApplication application = new InterviewResponse.InterviewApplication(
                interview.getApplication().getApplicationId(),
                interview.getApplication().getApplicant().getEmail(),
                interview.getApplication().getStatus(),
                interview.getApplication().getApplicant().getFullName()
        );

        response.setInterviewId(interview.getInterviewId());
        response.setScheduledAt(interview.getScheduledAt());
        response.setDurationMinutes(interview.getDurationMinutes());
        response.setType(interview.getType());
        response.setStatus(interview.getStatus());
        response.setLocation(interview.getLocation());
        response.setMeetingLink(interview.getMeetingLink());
        response.setNotes(interview.getNotes());
        response.setFeedback(interview.getFeedback());
        response.setRating(interview.getRating());
        response.setInterviewer(interviewer);
        response.setApplication(application);

        return response;
    }
}
