package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.request.recruiter.RecruiterUpdateRequest;
import iuh.fit.goat.dto.response.recruiter.RecruiterResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.JobService;
import iuh.fit.goat.service.RecruiterService;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RecruiterController {
//    private final RecruiterService recruiterService;
//    private final UserService userService;
//    private final PasswordEncoder passwordEncoder;
//    private final JobService jobService;
//
//    @PostMapping("/recruiters")
//    public ResponseEntity<RecruiterResponse> createRecruiter(
//            @Valid @RequestBody Recruiter recruiter
//    ) throws InvalidException {
//        if (userService.handleExistsByEmail(recruiter.getContact().getEmail())) {
//            throw new InvalidException("Email exists: " + recruiter.getContact().getEmail());
//        }
//
//        String hashPassword = passwordEncoder.encode(recruiter.getPassword());
//        recruiter.setPassword(hashPassword);
//
//        Recruiter newRecruiter = this.recruiterService.handleCreateRecruiter(recruiter);
//        RecruiterResponse recruiterResponse = this.recruiterService.convertToRecruiterResponse(newRecruiter);
//        return ResponseEntity.status(HttpStatus.CREATED).body(recruiterResponse);
//    }
//
//    @DeleteMapping("/recruiters/{id}")
//    @ApiMessage("Delete a recruiter")
//    public ResponseEntity<Void> deleteRecruiter(@PathVariable("id") long id) {
//        this.recruiterService.handleDeleteRecruiter(id);
//        return ResponseEntity.status(HttpStatus.OK).body(null);
//    }
//
//    @PutMapping("/recruiters")
//    public ResponseEntity<RecruiterResponse> updateRecruiter(
//            @Valid @RequestBody RecruiterUpdateRequest updateRequest
//    ) throws InvalidException {
//
//        Recruiter updatedRecruiter = this.recruiterService.handleUpdateRecruiter(updateRequest);
//
//        if (updatedRecruiter != null) {
//            RecruiterResponse recruiterResponse = this.recruiterService.convertToRecruiterResponse(updatedRecruiter);
//            return ResponseEntity.status(HttpStatus.OK).body(recruiterResponse);
//        } else {
//            throw new InvalidException("Recruiter not found");
//        }
//    }
//
//
//    @GetMapping("/recruiters/{id}")
//    public ResponseEntity<RecruiterResponse> getRecruiterById(@PathVariable("id") String id) throws InvalidException {
//        Pattern pattern = Pattern.compile("^[0-9]+$");
//
//        if (pattern.matcher(id).matches()) {
//            Recruiter recruiter = this.recruiterService.handleGetRecruiterById(Long.parseLong(id));
//            if (recruiter != null) {
//                RecruiterResponse recruiterResponse = this.recruiterService.convertToRecruiterResponse(recruiter);
//                return ResponseEntity.status(HttpStatus.OK).body(recruiterResponse);
//            } else {
//                throw new InvalidException("Recruiter not found");
//            }
//        } else {
//            throw new InvalidException("Id is number");
//        }
//    }
//
//    @GetMapping("/recruiters")
//    public ResponseEntity<ResultPaginationResponse> getAllRecruiters(
//            @Filter Specification<Recruiter> spec, Pageable pageable) {
//        Specification<Recruiter> baseSpec = (spec != null) ? spec : Specification.unrestricted();
//
//        Specification<Recruiter> finalSpec = baseSpec.and((root, query, criteriaBuilder) ->
//                criteriaBuilder.notEqual(root.get("userId"), 1L));
//
//        ResultPaginationResponse result = this.recruiterService.handleGetAllRecruiters(finalSpec, pageable);
//        return ResponseEntity.status(HttpStatus.OK).body(result);
//    }
//
//    @GetMapping("/recruiters/me")
//    @ApiMessage("Get current recruiter information")
//    public ResponseEntity<RecruiterResponse> getCurrentRecruiter() throws InvalidException {
//        Recruiter recruiter = this.recruiterService.handleGetCurrentRecruiter();
//
//        if (recruiter != null) {
//            RecruiterResponse recruiterResponse = this.recruiterService.convertToRecruiterResponse(recruiter);
//            return ResponseEntity.status(HttpStatus.OK).body(recruiterResponse);
//        } else {
//            throw new InvalidException("Current user is not a recruiter or not found");
//        }
//    }
//
//
//    @GetMapping("/recruiters/me/jobs")
//    public ResponseEntity<ResultPaginationResponse> getJobsForCurrentRecruiter(
//            @Filter Specification<Job> spec, Pageable pageable
//    ) {
//        ResultPaginationResponse result = this.jobService.handleGetCurrentRecruiterJobs(spec, pageable);
//        return ResponseEntity.status(HttpStatus.OK).body(result);
//    }
//
//    @GetMapping("/recruiters/{id}/jobs")
//    public ResponseEntity<ResultPaginationResponse> getJobsByRecruiterId(
//            @PathVariable("id") String id,
//            @Filter Specification<Job> spec,
//            Pageable pageable
//    ) throws InvalidException {
//        Pattern pattern = Pattern.compile("^[0-9]+$");
//        if (!pattern.matcher(id).matches()) {
//            throw new InvalidException("Id is number");
//        }
//        Long recruiterId = Long.parseLong(id);
//        ResultPaginationResponse result = this.jobService.handleGetJobsByRecruiterId(recruiterId, spec, pageable);
//        return ResponseEntity.status(HttpStatus.OK).body(result);
//    }
}
