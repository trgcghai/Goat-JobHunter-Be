package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.job.CreateJobRequest;
import iuh.fit.goat.dto.request.job.JobIdsActionRequest;
import iuh.fit.goat.dto.request.job.UpdateJobRequest;
import iuh.fit.goat.dto.response.job.JobActivateResponse;
import iuh.fit.goat.dto.response.job.JobApplicationCountResponse;
import iuh.fit.goat.dto.response.job.JobEnabledResponse;
import iuh.fit.goat.dto.response.job.JobResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Job;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

public interface JobService {
    JobResponse handleCreateJob(CreateJobRequest job);

    JobResponse handleUpdateJob(UpdateJobRequest job);

    void handleDeleteJob(JobIdsActionRequest request);

    Job handleGetJobById(long id);

    ResultPaginationResponse handleGetAllJobs(Specification<Job> spec, Pageable pageable);

    Map<Long, Long> handleCountAvailableJobByCompany();

    List<Long> handleGetAllJobIdsByCompany();

    ResultPaginationResponse handleGetCurrentCompanyJobs(Specification<Job> spec, Pageable pageable);

    List<JobResponse> handleGetAllAvailableJobsByCompanyId(Long companyId, Specification<Job> spec);

    ResultPaginationResponse handleGetJobsByCompanyId(Long companyId, Specification<Job> spec, Pageable pageable);

    JobResponse convertToJobResponse(Job job);

    List<JobActivateResponse> handleActivateJobs(List<Long> jobIds);

    List<JobActivateResponse> handleDeactivateJobs(List<Long> jobIds);

    List<JobApplicationCountResponse> handleCountApplicationsByJobIds(List<Long> jobIds);

    ResultPaginationResponse handleGetApplicantsForJob(Specification<Applicant> spec, Pageable pageable, Long jobId);

    ResultPaginationResponse handleGetApplicants(Specification<Applicant> spec, Pageable pageable);

    List<JobEnabledResponse> handleEnabledJobs(JobIdsActionRequest request);

    List<JobEnabledResponse> handleDisabledJobs(JobIdsActionRequest request);

    ResultPaginationResponse handleGetJobSubscribersByCurrentUser(Specification<Job> spec, Pageable pageable);

    ResultPaginationResponse handleGetRelatedJobsByCurrentUser(Specification<Job> spec, Pageable pageable);
}
