package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.resume.CreateResumeRequest;
import iuh.fit.goat.dto.response.resume.ResumeResponse;
import iuh.fit.goat.entity.Resume;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resumes")
public class ResumeController {
    private final ResumeService resumeService;

    @PostMapping
    public ResponseEntity<ResumeResponse> createResume(@Valid @RequestBody CreateResumeRequest request) throws InvalidException {
        Resume resume = this.resumeService.handleCreateResume(request);
        if(resume == null) throw new InvalidException("You should be logged in");

        return ResponseEntity.status(HttpStatus.CREATED).body(this.resumeService.handleConvertToResumeResponse(resume));
    }
}
