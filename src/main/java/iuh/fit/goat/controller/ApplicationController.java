package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import com.turkraft.springfilter.converter.FilterSpecification;
import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;
import com.turkraft.springfilter.parser.node.FilterNode;
import iuh.fit.goat.common.Status;
import iuh.fit.goat.dto.request.ApplicationIdsRequest;
import iuh.fit.goat.dto.response.ApplicationResponse;
import iuh.fit.goat.dto.response.ApplicationStatusResponse;
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


import java.util.List;
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
        Applicant applicant = this.applicationService.handleGetApplicant(application);

        application.setEmail(applicant.getContact().getEmail());
        ApplicationResponse applicationResponse = this.applicationService.handleCreateApplication(application);

        return ResponseEntity.status(HttpStatus.CREATED).body(applicationResponse);
    }

    @PutMapping("/applications/accepted")
    public ResponseEntity<List<ApplicationStatusResponse>> acceptApplications(
            @Valid @RequestBody ApplicationIdsRequest request
    ) {
        request.setStatus(Status.ACCEPTED.getValue());
        List<ApplicationStatusResponse> result = this.applicationService.handleUpdateApplication(request);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/applications/rejected")
    public ResponseEntity<List<ApplicationStatusResponse>> rejectApplications(
            @Valid @RequestBody ApplicationIdsRequest request
    ) {
        request.setStatus(Status.REJECTED.getValue());
        List<ApplicationStatusResponse> result = this.applicationService.handleUpdateApplication(request);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/applications/accept")
    public ResponseEntity<List<ApplicationStatusResponse>> acceptApplications(
            @Valid @RequestBody ApplicationIdsRequest request
    ) {
        List<ApplicationStatusResponse> result = this.applicationService.handleAcceptApplications(request);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PutMapping("/applications/reject")
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
}
