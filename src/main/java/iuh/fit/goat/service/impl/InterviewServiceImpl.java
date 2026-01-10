package iuh.fit.goat.service.impl;

import iuh.fit.goat.component.redis.interview.InterviewEventProducer;
import iuh.fit.goat.dto.request.interview.CreateInterviewRequest;
import iuh.fit.goat.dto.request.interview.InterviewIdsRequest;
import iuh.fit.goat.dto.response.interview.InterviewResponse;
import iuh.fit.goat.dto.response.interview.InterviewStatusResponse;
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
import java.util.Map;
import java.util.stream.Collectors;

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

        Map<String, List<InterviewResponse>> interviewsByEmail =
                responses.stream().collect(Collectors.groupingBy(
                        interview -> interview.getApplication().getEmail()
                ));

        interviewsByEmail.forEach((email, interviewResponses) -> {
            if(interviewResponses.isEmpty()) return;
            this.eventProducer.publishInterviewCreated(email, interviewResponses, "");
        });

        return responses;
    }

    @Override
    public List<InterviewStatusResponse> handleCompleteInterviews(InterviewIdsRequest request) {
        List<Interview> interviews = this.interviewRepository.findAllById(request.getInterviewIds());
        if(interviews.isEmpty()) return Collections.emptyList();

        List<Interview> scheduledInterviews = interviews.stream()
                .filter(interview -> interview.getStatus() == InterviewStatus.SCHEDULED)
                .toList();
        scheduledInterviews.forEach(interview -> interview.setStatus(InterviewStatus.COMPLETED));
        this.interviewRepository.saveAll(scheduledInterviews);

        List<InterviewResponse> responses = scheduledInterviews.stream()
                .map(this::handleConvertToInterviewResponse)
                .toList();
        Map<String, List<InterviewResponse>> interviewsByEmail =
                responses.stream().collect(Collectors.groupingBy(
                        interview -> interview.getApplication().getEmail()
                ));
        interviewsByEmail.forEach((email, interviewResponses) -> {
            if(interviewResponses.isEmpty()) return;
            this.eventProducer.publishInterviewCreated(email, interviewResponses, "");
        });

        return scheduledInterviews.stream()
                .map(interview -> new InterviewStatusResponse(
                        interview.getInterviewId(),
                        interview.getStatus().getValue()
                )).toList();
    }

    @Override
    public List<InterviewStatusResponse> handleCancelInterviews(InterviewIdsRequest request) {
        List<Interview> interviews = this.interviewRepository.findAllById(request.getInterviewIds());
        if(interviews.isEmpty()) return Collections.emptyList();

        List<Interview> scheduledInterviews = interviews.stream()
                .filter(interview -> interview.getStatus() == InterviewStatus.SCHEDULED)
                .toList();
        scheduledInterviews.forEach(interview -> interview.setStatus(InterviewStatus.CANCELLED));
        this.interviewRepository.saveAll(scheduledInterviews);

        List<InterviewResponse> responses = scheduledInterviews.stream()
                .map(this::handleConvertToInterviewResponse)
                .toList();
        Map<String, List<InterviewResponse>> interviewsByEmail =
                responses.stream().collect(Collectors.groupingBy(
                        interview -> interview.getApplication().getEmail()
                ));
        interviewsByEmail.forEach((email, interviewResponses) -> {
            if(interviewResponses.isEmpty()) return;
            this.eventProducer.publishInterviewCreated(email, interviewResponses, request.getReason());
        });

        return scheduledInterviews.stream()
                .map(interview -> new InterviewStatusResponse(
                        interview.getInterviewId(),
                        interview.getStatus().getValue()
                )).toList();
    }

    @Override
    public List<InterviewStatusResponse> handleRescheduleInterviews(InterviewIdsRequest request) {
        List<Interview> interviews = this.interviewRepository.findAllById(request.getInterviewIds());
        if(interviews.isEmpty()) return Collections.emptyList();

        List<Interview> scheduledInterviews = interviews.stream()
                .filter(interview -> interview.getStatus() == InterviewStatus.SCHEDULED)
                .toList();
        scheduledInterviews.forEach(interview -> interview.setStatus(InterviewStatus.RESCHEDULED));
        this.interviewRepository.saveAll(scheduledInterviews);

        List<InterviewResponse> responses = scheduledInterviews.stream()
                .map(this::handleConvertToInterviewResponse)
                .toList();
        Map<String, List<InterviewResponse>> interviewsByEmail =
                responses.stream().collect(Collectors.groupingBy(
                        interview -> interview.getApplication().getEmail()
                ));
        interviewsByEmail.forEach((email, interviewResponses) -> {
            if(interviewResponses.isEmpty()) return;
            this.eventProducer.publishInterviewCreated(email, interviewResponses, request.getReason());
        });

        return scheduledInterviews.stream()
                .map(interview -> new InterviewStatusResponse(
                        interview.getInterviewId(),
                        interview.getStatus().getValue()
                )).toList();
    }

    @Override
    public Interview handleGetInterviewById(Long interviewId) {
        return this.interviewRepository.findById(interviewId).orElse(null);
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
