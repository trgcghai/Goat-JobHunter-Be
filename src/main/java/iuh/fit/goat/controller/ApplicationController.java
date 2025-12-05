package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import com.turkraft.springfilter.converter.FilterSpecification;
import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;
import com.turkraft.springfilter.parser.node.FilterNode;
import iuh.fit.goat.dto.request.application.ApplicationIdsRequest;
import iuh.fit.goat.dto.response.application.ApplicationResponse;
import iuh.fit.goat.dto.response.application.ApplicationStatusResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import iuh.fit.goat.exception.*;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.service.*;
import iuh.fit.goat.util.*;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;
    private final JobService jobService;
    private final FilterSpecificationConverter filterSpecificationConverter;
    private final FilterParser filterParser;

    @PostMapping("/applications")
    public ResponseEntity<ApplicationResponse> createApplication(@Valid @RequestBody Application application)
            throws InvalidException {
        boolean checkUserAndJob = this.applicationService.checkApplicantAndJobExist(application);
        if(!checkUserAndJob){
            throw new InvalidException("User or Job doesn't exist");
        }

        boolean checkCanApplyToJob = this.applicationService.handleCanApplyToJob(
                application.getApplicant().getUserId(),
                application.getJob().getJobId()
        );
        if(!checkCanApplyToJob){
            throw new InvalidException("You can submit a maximum of 3 applications for this job.");
        }

        Applicant applicant = this.applicationService.handleGetApplicant(application);

        application.setEmail(applicant.getContact().getEmail());
        ApplicationResponse applicationResponse = this.applicationService.handleCreateApplication(application);

        return ResponseEntity.status(HttpStatus.CREATED).body(applicationResponse);
    }

    @PutMapping("/applications/accepted")
    public ResponseEntity<List<ApplicationStatusResponse>> acceptApplications(
            @Valid @RequestBody ApplicationIdsRequest request
    ) {
        List<ApplicationStatusResponse> result = this.applicationService.handleAcceptApplications(request);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PutMapping("/applications/rejected")
    public ResponseEntity<List<ApplicationStatusResponse>> rejectApplications(
            @Valid @RequestBody ApplicationIdsRequest request
    ) {
        List<ApplicationStatusResponse> result = this.applicationService.handleRejectApplications(request);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @DeleteMapping("/applications/{id}")
    @ApiMessage("Delete a application")
    public ResponseEntity<Void> deleteApplication(@PathVariable("id") long id) {
        this.applicationService.handleDeleteApplication(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(@PathVariable("id") String id)
            throws InvalidException {
        Pattern pattern = Pattern.compile("^[0-9]+$");
        if (pattern.matcher(id).matches()) {
            Application currentApplication = this.applicationService.handleGetApplicationById(Long.parseLong(id));
            if (currentApplication != null) {
                ApplicationResponse applicationResponse = this.applicationService
                        .convertToApplicationResponse(currentApplication);
                return ResponseEntity.status(HttpStatus.OK).body(applicationResponse);
            } else {
                throw new InvalidException("Application doesn't exist");
            }
        } else {
            throw new InvalidException("Id is number");
        }
    }

    @GetMapping("/all-applications")
    public ResponseEntity<ResultPaginationResponse> getAllApplications(
            @Filter Specification<Application> spec, Pageable pageable) {
        ResultPaginationResponse result = this.applicationService.handleGetAllApplications(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/applications")
    public ResponseEntity<ResultPaginationResponse> getAllApplicationsByRecruiter(
            @Filter Specification<Application> spec, Pageable pageable) {
        List<Long> jobIds = this.jobService.handleGetAllJobIdsByRecruiter();
        if(jobIds == null) return null;

        Specification<Application> specification = (root, query, cb) ->
                root.get("job").get("jobId").in(jobIds);
        Specification<Application> finalSpec = spec.and(specification);

        ResultPaginationResponse result = this.applicationService.handleGetAllApplications(finalSpec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/applications/by-applicant")
    public ResponseEntity<ResultPaginationResponse> getAllApplicationsByApplicant(Pageable pageable) {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";

        FilterNode filterNode = this.filterParser.parse("email='" + email + "'");
        FilterSpecification<Application> spec = this.filterSpecificationConverter.convert(filterNode);

        ResultPaginationResponse result = this.applicationService.handleGetAllApplications(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/applications/by-applicant/{applicantId}")
    public ResponseEntity<ResultPaginationResponse> getApplicationsByApplicantId(
            @PathVariable("applicantId") String applicantId,
            Pageable pageable) throws InvalidException {
        Pattern pattern = Pattern.compile("^[0-9]+$");
        if (!pattern.matcher(applicantId).matches()) {
            throw new InvalidException("Id is number");
        }

        long id = Long.parseLong(applicantId);
        Specification<Application> specification = (root, query, cb) ->
                cb.equal(root.get("applicant").get("userId"), id);

        ResultPaginationResponse result = this.applicationService.handleGetAllApplications(specification, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/applications/count")
    public ResponseEntity<Map<String, Object>> countApplications(
            @RequestParam Long applicantId,
            @RequestParam Long jobId
    ) {
        Long count = this.applicationService.handleCountApplicationsByApplicantForJob(applicantId, jobId);
        Map<String, Object> response = new HashMap<>();
        response.put("submittedApplications", count);

        return ResponseEntity.ok(response);
    }
}
