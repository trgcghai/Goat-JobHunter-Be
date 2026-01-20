package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.request.job.CreateJobRequest;
import iuh.fit.goat.dto.request.job.JobIdsActionRequest;
import iuh.fit.goat.dto.request.job.JobIdsRequest;
import iuh.fit.goat.dto.request.job.UpdateJobRequest;
import iuh.fit.goat.dto.response.job.JobActivateResponse;
import iuh.fit.goat.dto.response.job.JobApplicationCountResponse;
import iuh.fit.goat.dto.response.job.JobEnabledResponse;
import iuh.fit.goat.dto.response.job.JobResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.JobService;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {
    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest job) {
        JobResponse newJob = this.jobService.handleCreateJob(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(newJob);
    }

    @PutMapping
    public ResponseEntity<JobResponse> updateJob(@Valid @RequestBody UpdateJobRequest job) throws InvalidException {
        Job currentJob = this.jobService.handleGetJobById(job.getJobId());
        if(currentJob != null){
            JobResponse updateJob = this.jobService.handleUpdateJob(job);
            return ResponseEntity.status(HttpStatus.OK).body(updateJob);
        } else {
            throw new InvalidException("Job doesn't exist");
        }
    }

    @PutMapping("/activate")
    public ResponseEntity<List<JobActivateResponse>> activateJobs(@Valid @RequestBody JobIdsRequest request) {
        List<JobActivateResponse> result = this.jobService.handleActivateJobs(request.getJobIds());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/deactivate")
    public ResponseEntity<List<JobActivateResponse>> deactivateJobs(@Valid @RequestBody JobIdsRequest request) {
        List<JobActivateResponse> result = this.jobService.handleDeactivateJobs(request.getJobIds());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteJob(@Valid @RequestBody JobIdsActionRequest request) {
        this.jobService.handleDeleteJob(request);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable("id") String id) throws InvalidException {
        if(!SecurityUtil.checkValidNumber(id)){
            throw new InvalidException("Id is number");
        }

        Job currentJob = this.jobService.handleGetJobById(Long.parseLong(id));
        if (currentJob == null) {
            throw new InvalidException("Job doesn't exist");
        }

        return ResponseEntity.status(HttpStatus.OK).body(this.jobService.convertToJobResponse(currentJob));
    }

    @GetMapping
    public ResponseEntity<ResultPaginationResponse> getAllJobs(
            @Filter Specification<Job> spec, Pageable pageable
    ) {
        ResultPaginationResponse result = this.jobService.handleGetAllJobs(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/available")
    public ResponseEntity<ResultPaginationResponse> getAllAvailableJobs(
            @Filter Specification<Job> spec, Pageable pageable
    ) {
        Specification<Job> baseSpec = (spec != null) ? spec : Specification.unrestricted();
        Specification<Job> finalSpec = baseSpec
                .and((root, query, criteriaBuilder)
                        -> criteriaBuilder.and(
                                criteriaBuilder.isTrue(root.get("enabled")),
                                criteriaBuilder.isNull(root.get("deletedAt"))
                        )
                );

        ResultPaginationResponse result = this.jobService.handleGetAllJobs(finalSpec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/companies/count")
    public ResponseEntity<Map<Long, Long>> countAvailableJobByCompany() {
        Map<Long, Long> result = this.jobService.handleCountAvailableJobByCompany();
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/count-applications")
    public ResponseEntity<List<JobApplicationCountResponse>> countApplications(
            @RequestParam("jobIds") String jobIdsCsv
    ) throws InvalidException {
        if (jobIdsCsv == null || jobIdsCsv.trim().isEmpty()) {
            throw new InvalidException("jobIds is required");
        }

        String[] parts = jobIdsCsv.split(",");
        List<Long> jobIds = new ArrayList<>(parts.length);
        try {
            for (String p : parts) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    jobIds.add(Long.parseLong(trimmed));
                }
            }
        } catch (NumberFormatException ex) {
            throw new InvalidException("jobIds must be comma separated numbers");
        }

        if (jobIds.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<JobApplicationCountResponse> result = this.jobService.handleCountApplicationsByJobIds(jobIds);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{jobId}/applicants")
    public ResponseEntity<ResultPaginationResponse> getSuitableApplicantsForJob(
            @Filter Specification<Applicant> spec, Pageable pageable,
            @PathVariable("jobId") String jobId
    ) throws InvalidException {
        if(!SecurityUtil.checkValidNumber(jobId)){
            ResultPaginationResponse result = this.jobService.handleGetApplicants(spec, pageable);
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }

        Job currentJob = this.jobService.handleGetJobById(Long.parseLong(jobId));
        if (currentJob == null) {
            throw new InvalidException("Job doesn't exist");
        }

        ResultPaginationResponse result = this.jobService.handleGetApplicantsForJob(spec, pageable, Long.parseLong(jobId));
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PutMapping("/enabled")
    public ResponseEntity<List<JobEnabledResponse>> enableJobs(
            @Valid @RequestBody JobIdsActionRequest request
    ) {
        List<JobEnabledResponse> result = this.jobService.handleEnabledJobs(request);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PutMapping("/disabled")
    public ResponseEntity<List<JobEnabledResponse>> disableJobs(
            @Valid @RequestBody JobIdsActionRequest request
    ) {
        List<JobEnabledResponse> result = this.jobService.handleDisabledJobs(request);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
