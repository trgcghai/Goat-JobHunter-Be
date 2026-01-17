package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.recruiter.RecruiterUpdateRequest;
import iuh.fit.goat.dto.response.recruiter.RecruiterResponse;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.RecruiterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recruiters")
@RequiredArgsConstructor
public class RecruiterController {
    private final RecruiterService recruiterService;

    @PutMapping
    public ResponseEntity<RecruiterResponse> updateRecruiter(
            @Valid @RequestBody RecruiterUpdateRequest updateRequest
    ) throws InvalidException {

        Recruiter updatedRecruiter = this.recruiterService.handleUpdateRecruiter(updateRequest);

        if (updatedRecruiter != null) {
            RecruiterResponse recruiterResponse = this.recruiterService.convertToRecruiterResponse(updatedRecruiter);
            return ResponseEntity.status(HttpStatus.OK).body(recruiterResponse);
        } else {
            throw new InvalidException("Recruiter not found");
        }
    }

}
