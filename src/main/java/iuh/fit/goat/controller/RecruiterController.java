package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.recruiter.RecruiterUpdateRequest;
import iuh.fit.goat.dto.response.recruiter.RecruiterResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.PermissionException;
import iuh.fit.goat.service.RecruiterService;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/recruiters")
@RequiredArgsConstructor
public class RecruiterController {
    private final RecruiterService recruiterService;
    private final UserService userService;

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecruiterResponse> updateRecruiter(
            @Valid @ModelAttribute RecruiterUpdateRequest updateRequest
    ) throws InvalidException, PermissionException {

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

        Recruiter updatedRecruiter = this.recruiterService.handleUpdateRecruiter(updateRequest);
        if (updatedRecruiter == null) {
            throw new InvalidException("Recruiter not found");
        }

        RecruiterResponse recruiterResponse = this.recruiterService.convertToRecruiterResponse(updatedRecruiter);

        return ResponseEntity.status(HttpStatus.OK).body(recruiterResponse);
    }

}
