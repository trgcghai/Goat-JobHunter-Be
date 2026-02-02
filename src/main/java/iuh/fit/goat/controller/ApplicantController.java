package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.applicant.ApplicantUpdateRequest;
import iuh.fit.goat.dto.response.applicant.ApplicantResponse;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import iuh.fit.goat.exception.*;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.service.*;

@RestController
@RequestMapping("/api/v1/applicants")
@RequiredArgsConstructor
public class ApplicantController {
    private final ApplicantService applicantService;

    @PutMapping
    public ResponseEntity<ApplicantResponse> updateApplicant(
            @Valid @RequestBody ApplicantUpdateRequest updateRequest
    ) throws InvalidException
    {
        Applicant updatedApplicant = this.applicantService.handleUpdateApplicant(updateRequest);

        if (updatedApplicant != null) {
            ApplicantResponse applicantResponse = this.applicantService.convertToApplicantResponse(updatedApplicant);
            return ResponseEntity.status(HttpStatus.OK).body(applicantResponse);
        } else {
            throw new InvalidException("Applicant not found");
        }
    }

    @PutMapping("/availableStatus")
    public ResponseEntity<ApplicantResponse> toggleAvailableStatus() throws InvalidException
    {
        Applicant updatedApplicant = this.applicantService.handleToggleAvailableStatus();
        ApplicantResponse applicantResponse = this.applicantService.convertToApplicantResponse(updatedApplicant);
        return ResponseEntity.status(HttpStatus.OK).body(applicantResponse);
    }
}
