package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.interview.CreateInterviewRequest;
import iuh.fit.goat.dto.response.interview.InterviewResponse;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.InterviewService;
import iuh.fit.goat.service.RecruiterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/interviews")
public class InterviewController {
    private final InterviewService interviewService;
    private final RecruiterService recruiterService;

    @PostMapping
    public ResponseEntity<List<InterviewResponse>> createInterviews(
            @Valid @RequestBody CreateInterviewRequest request
    ) throws InvalidException {
        Recruiter recruiter = this.recruiterService.handleGetRecruiterById(request.getInterviewerId());
        if(recruiter == null) throw new InvalidException("Recruiter not found");

        return ResponseEntity.status(HttpStatus.CREATED).body(this.interviewService.handleCreateInterviews(request));
    }
}
