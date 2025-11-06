package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.response.ApplicantResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import iuh.fit.goat.exception.*;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.service.*;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApplicantController {
    private final ApplicantService applicantService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/applicants")
    public ResponseEntity<ApplicantResponse> createApplicant(@Valid @RequestBody Applicant applicant) throws InvalidException {
        if(this.userService.handleExistsByEmail(applicant.getContact().getEmail())) {
            throw new InvalidException("Email exists: " + applicant.getContact().getEmail());
        }

        String hashPassword = this.passwordEncoder.encode(applicant.getPassword());
        applicant.setPassword(hashPassword);

        Applicant newApplicant = this.applicantService.handleCreateApplicant(applicant);
        ApplicantResponse applicantResponse = this.applicantService.convertToApplicantResponse(newApplicant);
        return ResponseEntity.status(HttpStatus.CREATED).body(applicantResponse);
    }

    @DeleteMapping("/applicants/{id}")
    @ApiMessage("Delete a applicant")
    public ResponseEntity<Void> deleteApplicant(@PathVariable("id") long id) {
        this.applicantService.handleDeleteApplicant(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PutMapping("/applicants")
    public ResponseEntity<ApplicantResponse> updateApplicant(@Valid @RequestBody Applicant applicant)
            throws InvalidException {
        Applicant updatedApplicant = this.applicantService.handleUpdateApplicant(applicant);

        if(updatedApplicant != null) {
            ApplicantResponse applicantResponse = this.applicantService.convertToApplicantResponse(updatedApplicant);
            return ResponseEntity.status(HttpStatus.OK).body(applicantResponse);
        } else {
            throw new InvalidException("Applicant not found");
        }
    }

    @GetMapping("/applicants/{id}")
    public ResponseEntity<ApplicantResponse> getApplicantById(@PathVariable("id") String id) throws InvalidException {
        Pattern pattern = Pattern.compile("^[0-9]+$");

        if(pattern.matcher(id).matches()){
            Applicant applicant = this.applicantService.handleGetApplicantById(Long.parseLong(id));
            if(applicant != null) {
                ApplicantResponse applicantResponse = this.applicantService.convertToApplicantResponse(applicant);
                return ResponseEntity.status(HttpStatus.OK).body(applicantResponse);
            } else {
                throw new InvalidException("Applicant not found");
            }
        } else {
            throw new InvalidException("Id is number");
        }
    }

    @GetMapping("/applicants")
    public ResponseEntity<ResultPaginationResponse> getAllApplicants(
            @Filter Specification<Applicant> spec,
            Pageable pageable
    ) {
        ResultPaginationResponse result = this.applicantService.handleGetAllApplicants(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
