package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.applicant.ApplicantUpdateRequest;
import iuh.fit.goat.dto.response.applicant.ApplicantResponse;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import iuh.fit.goat.exception.*;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.service.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/applicants")
@RequiredArgsConstructor
public class ApplicantController {
    private final ApplicantService applicantService;
    private final UserService userService;

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApplicantResponse> updateApplicant(
            @Valid @ModelAttribute ApplicantUpdateRequest updateRequest
    ) throws InvalidException, PermissionException
    {
        String currentEmail = SecurityUtil.getCurrentUserEmail();
        if (currentEmail == null || currentEmail.isBlank()) {
            throw new PermissionException("User not authenticated");
        }

        Account currentAccount = this.userService.handleGetAccountByEmail(currentEmail);
        if (currentAccount == null) {
            throw new PermissionException("User not authenticated");
        }
        if (!Objects.equals(currentAccount.getAccountId(), updateRequest.getAccountId())) {
            throw new PermissionException("You can only update your own profile");
        }

        Applicant updatedApplicant = this.applicantService.handleUpdateApplicant(updateRequest);
        if (updatedApplicant == null) {
            throw new InvalidException("Applicant not found");
        }

        ApplicantResponse applicantResponse = this.applicantService.convertToApplicantResponse(updatedApplicant);

        return ResponseEntity.status(HttpStatus.OK).body(applicantResponse);
    }

    @PutMapping("/availableStatus")
    public ResponseEntity<ApplicantResponse> toggleAvailableStatus() throws InvalidException
    {
        Applicant updatedApplicant = this.applicantService.handleToggleAvailableStatus();
        ApplicantResponse applicantResponse = this.applicantService.convertToApplicantResponse(updatedApplicant);
        return ResponseEntity.status(HttpStatus.OK).body(applicantResponse);
    }
}
