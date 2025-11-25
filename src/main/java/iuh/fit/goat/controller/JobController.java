package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.request.CreateJobRequest;
import iuh.fit.goat.dto.request.JobActivateRequest;
import iuh.fit.goat.dto.request.UpdateJobRequest;
import iuh.fit.goat.dto.response.JobActivateResponse;
import iuh.fit.goat.dto.response.JobResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class JobController {
    private final JobService jobService;

    @PostMapping("/jobs")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest job) {
        JobResponse newJob = this.jobService.handleCreateJob(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(newJob);
    }

    @PutMapping("/jobs")
    public ResponseEntity<JobResponse> updateJob(@Valid @RequestBody UpdateJobRequest job) throws InvalidException {
        Job currentJob = this.jobService.handleGetJobById(job.getJobId());
        if(currentJob != null){
            JobResponse updateJob = this.jobService.handleUpdateJob(job);
            return ResponseEntity.status(HttpStatus.OK).body(updateJob);
        } else {
            throw new InvalidException("Job doesn't exist");
        }
    }

    @PutMapping("/jobs/activate")
    public ResponseEntity<List<JobActivateResponse>> activateJobs(@Valid @RequestBody JobActivateRequest request) {
        List<JobActivateResponse> result = this.jobService.handleActivateJobs(request.getJobIds());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/jobs/deactivate")
    public ResponseEntity<List<JobActivateResponse>> deactivateJobs(@Valid @RequestBody JobActivateRequest request) {
        List<JobActivateResponse> result = this.jobService.handleDeactivateJobs(request.getJobIds());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable("id") String id) throws InvalidException {
        Pattern pattern = Pattern.compile("^[0-9]+$");
        if (pattern.matcher(id).matches()) {
            if (this.jobService.handleGetJobById(Long.parseLong(id)) != null) {
                this.jobService.handleDeleteJob(Long.parseLong(id));
                return ResponseEntity.status(HttpStatus.OK).body(null);
            } else {
                throw new InvalidException("Job doesn't exist");
            }
        } else {
            throw new InvalidException("Id is number");
        }
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable("id") String id) throws InvalidException {
        Pattern pattern = Pattern.compile("^[0-9]+$");
        if (pattern.matcher(id).matches()) {
            Job currentJob = this.jobService.handleGetJobById(Long.parseLong(id));
            if (currentJob == null) {
                throw new InvalidException("Job doesn't exist");
            }
            return ResponseEntity.status(HttpStatus.OK).body(this.jobService.convertToJobResponse(currentJob));
        } else {
            throw new InvalidException("Id is number");
        }
    }

    @GetMapping("/jobs")
    public ResponseEntity<ResultPaginationResponse> getAllJobs(
            @Filter Specification<Job> spec, Pageable pageable
    ) {
        ResultPaginationResponse result = this.jobService.handleGetAllJobs(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/jobs/recruiters/count")
    public ResponseEntity<Map<Long, Long>> countJobByRecruiter() {
        Map<Long, Long> result = this.jobService.handleCountJobByRecruiter();
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
