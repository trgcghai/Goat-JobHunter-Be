package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.JobResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Job;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

public interface JobService {
    JobResponse handleCreateJob(Job job);

    JobResponse handleUpdateJob(Job job);

    void handleDeleteJob(long id);

    Job handleGetJobById(long id);

    ResultPaginationResponse handleGetAllJobs(Specification<Job> spec, Pageable pageable);

    Map<Long, Long> handleCountJobByRecruiter();

    List<Long> handleGetAllJobIdsByRecruiter();

    ResultPaginationResponse handleGetCurrentRecruiterJobs(Specification<Job> spec, Pageable pageable);

    ResultPaginationResponse handleGetJobsByRecruiterId(Long recruiterId, Specification<Job> spec, Pageable pageable);

    JobResponse convertToJobResponse(Job job);
}
