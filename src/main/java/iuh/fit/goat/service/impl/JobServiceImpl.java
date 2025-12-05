package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.Level;
import iuh.fit.goat.common.WorkingType;
import iuh.fit.goat.dto.request.job.CreateJobRequest;
import iuh.fit.goat.dto.request.job.UpdateJobRequest;
import iuh.fit.goat.dto.response.applicant.ApplicantResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.job.JobActivateResponse;
import iuh.fit.goat.dto.response.job.JobApplicationCountResponse;
import iuh.fit.goat.dto.response.job.JobResponse;
import iuh.fit.goat.service.ApplicantService;
import iuh.fit.goat.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.util.SecurityUtil;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {
    private final ApplicantService applicantService;
    private final ApplicantRepository applicantRepository;
    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final CareerRepository careerRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final SubscriberRepository subscriberRepository;

    @Override
    public JobResponse handleCreateJob(CreateJobRequest request) {
        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setLocation(request.getLocation());
        job.setSalary(request.getSalary());
        job.setQuantity(request.getQuantity());
        job.setDescription(request.getDescription());
        job.setLevel(Level.valueOf(request.getLevel()));
        job.setStartDate(request.getStartDate());
        job.setEndDate(request.getEndDate());
        job.setActive(request.getActive() != null ? request.getActive() : false);
        job.setWorkingType(WorkingType.valueOf(request.getWorkingType()));

        if (request.getSkillIds() != null && !request.getSkillIds().isEmpty()) {
            List<Skill> skills = this.skillRepository.findBySkillIdIn(request.getSkillIds());
            job.setSkills(skills);
        }

        if (request.getRecruiterId() != null) {
            Optional<User> currentUser = this.userRepository.findById(request.getRecruiterId());
            if (currentUser.isPresent() && currentUser.get() instanceof Recruiter recruiter) {
                job.setRecruiter(recruiter);
            }
        }

        if (request.getCareerId() != null) {
            Optional<Career> currentCareer = this.careerRepository.findById(request.getCareerId());
            currentCareer.ifPresent(job::setCareer);
        }

        Job res = this.jobRepository.save(job);

        JobResponse jobResponse = convertToJobResponse(res);
        jobResponse.setCreatedAt(res.getCreatedAt());
        jobResponse.setCreatedBy(res.getCreatedBy());

        return jobResponse;
    }

    @Override
    public JobResponse handleUpdateJob(UpdateJobRequest request) {
        Job currentJob = this.handleGetJobById(request.getJobId());

        if (currentJob == null) {
            return null;
        }

        if (request.getSkillIds() != null) {
            List<Skill> skills = this.skillRepository.findBySkillIdIn(request.getSkillIds());
            currentJob.setSkills(skills);
        }

        if (request.getCareerId() != null) {
            Optional<Career> currentCareer = this.careerRepository.findById(request.getCareerId());
            currentCareer.ifPresent(currentJob::setCareer);
        }

        if (request.getDescription() != null) currentJob.setDescription(request.getDescription());
        if (request.getStartDate() != null) currentJob.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) currentJob.setEndDate(request.getEndDate());
        if (request.getActive() != null) currentJob.setActive(request.getActive());
        if (request.getLevel() != null) currentJob.setLevel(Level.valueOf(request.getLevel()));
        if (request.getQuantity() != null) currentJob.setQuantity(request.getQuantity());
        if (request.getSalary() != null) currentJob.setSalary(request.getSalary());
        if (request.getTitle() != null) currentJob.setTitle(request.getTitle());
        if (request.getWorkingType() != null) currentJob.setWorkingType(WorkingType.valueOf(request.getWorkingType()));
        if (request.getLocation() != null) currentJob.setLocation(request.getLocation());

        Job res = this.jobRepository.save(currentJob);

        JobResponse jobResponse = convertToJobResponse(res);
        jobResponse.setUpdatedAt(res.getUpdatedAt());
        jobResponse.setUpdatedBy(res.getUpdatedBy());

        return jobResponse;
    }

    @Override
    public void handleDeleteJob(long id) {
        Job job = this.handleGetJobById(id);

        if(job.getApplications() != null){
            List<Application> applications = this.applicationRepository.findByJob(job);
            this.applicationRepository.deleteAll(applications);
        }
        if(job.getUsers() != null){
            job.getUsers().forEach(user -> {user.getSavedJobs().remove(job);});
        }

        this.jobRepository.deleteById(id);
    }

    @Override
    public Job handleGetJobById(long id) {
        Optional<Job> job = this.jobRepository.findById(id);

        return job.orElse(null);
    }

    @Override
    public ResultPaginationResponse handleGetAllJobs(Specification<Job> spec, Pageable pageable) {
        Page<Job> page = this.jobRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<JobResponse> responses = page.getContent().stream()
                .map(this::convertToJobResponse)
                .toList();

        return new ResultPaginationResponse(meta, responses);
    }

    @Override
    public Map<Long, Long> handleCountJobByRecruiter(){
        return this.jobRepository.countJobs()
                .stream().collect(
                        Collectors.toMap(
                                row-> (Long)row[0],
                                row-> (Long)row[1]
                        )
                );
    }

    @Override
    public List<Long> handleGetAllJobIdsByRecruiter() {
        List<Long> jobIds = null;

        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        User currentUser = this.userRepository.findByContact_Email(email);
        if(currentUser instanceof Recruiter){
            List<Job> jobs = ( (Recruiter) currentUser ).getJobs();
            if(jobs != null && !jobs.isEmpty()){
                jobIds = jobs.stream().map(Job::getJobId).toList();
            }
        }

        return jobIds;
    }

    @Override
    public ResultPaginationResponse handleGetCurrentRecruiterJobs(Specification<Job> spec, Pageable pageable) {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        if (email.isEmpty()) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }
        User currentUser = this.userRepository.findByContact_Email(email);
        if (!(currentUser instanceof Recruiter)) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }
        Long recruiterId = currentUser.getUserId();
        return handleGetJobsByRecruiterId(recruiterId, spec, pageable);
    }

    @Override
    public ResultPaginationResponse handleGetJobsByRecruiterId(Long recruiterId, Specification<Job> spec, Pageable pageable) {
        if (recruiterId == null) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        Specification<Job> recruiterSpec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("recruiter").get("userId"), recruiterId);

        Specification<Job> finalSpec = (spec != null) ? spec.and(recruiterSpec) : recruiterSpec;

        Page<Job> page = this.jobRepository.findAll(finalSpec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<JobResponse> responses = page.getContent().stream()
                .map(this::convertToJobResponse)
                .toList();

        return new ResultPaginationResponse(meta, responses);
    }

    @Override
    public List<JobActivateResponse> handleActivateJobs(List<Long> jobIds) {
        return handleSetActiveForJobs(jobIds, true);
    }

    @Override
    public List<JobActivateResponse> handleDeactivateJobs(List<Long> jobIds) {
        return handleSetActiveForJobs(jobIds, false);
    }

    @Override
    public List<JobApplicationCountResponse> handleCountApplicationsByJobIds(List<Long> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Job> existingJobs = this.jobRepository.findAllById(jobIds);
        Map<Long, Job> jobMap = existingJobs.stream()
                .collect(Collectors.toMap(Job::getJobId, j -> j));

        List<JobApplicationCountResponse> results = new ArrayList<>(jobIds.size());

        for (Long id : jobIds) {
            Job job = jobMap.get(id);
            if (job == null) {
                results.add(new JobApplicationCountResponse(id, 0L));
                continue;
            }

            List<Application> apps = this.applicationRepository.findByJob(job);
            long count = (apps == null) ? 0L : apps.size();
            results.add(new JobApplicationCountResponse(id, count));
        }

        return results;
    }

    @Override
    public ResultPaginationResponse handleGetApplicantsForJob(Specification<Applicant> spec, Pageable pageable, Long jobId) {
        Job job = this.handleGetJobById(jobId);

        List<Applicant> applicants = this.applicantRepository.findAll(spec);

        List<Subscriber> subscribers = this.subscriberRepository.findAll();
        Map<String, Subscriber> emailSubscriberMap = subscribers.stream()
                .collect(Collectors.toMap(Subscriber::getEmail, s -> s));

        List<Applicant> matchedApplicants = applicants.stream()
                .filter(a -> {
                    boolean levelMatch = a.getLevel() != null && a.getLevel() == job.getLevel();

                    Subscriber subscriber = emailSubscriberMap.get(a.getContact().getEmail());
                    boolean skillMatch = false;
                    if (subscriber != null && subscriber.getSkills() != null && !subscriber.getSkills().isEmpty()) {
                        skillMatch = subscriber.getSkills().stream()
                                .anyMatch(job.getSkills()::contains);
                    }

                    return (levelMatch || skillMatch) && a.isAvailableStatus(); // only include available applicants
                })
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), matchedApplicants.size());
        List<Applicant> pageContent = start >= matchedApplicants.size() ? List.of() : matchedApplicants.subList(start, end);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages((int) Math.ceil((double) matchedApplicants.size() / pageable.getPageSize()));
        meta.setTotal(matchedApplicants.size());

        List<ApplicantResponse> result = pageContent.stream()
                .map(this.applicantService::convertToApplicantResponse)
                .toList();

        return new ResultPaginationResponse(meta, result);
    }

    @Override
    public JobResponse convertToJobResponse(Job job) {
        JobResponse jobResponse = new JobResponse();

        jobResponse.setJobId(job.getJobId());
        jobResponse.setTitle(job.getTitle());
        jobResponse.setLocation(job.getLocation());
        jobResponse.setSalary(job.getSalary());
        jobResponse.setQuantity(job.getQuantity());
        jobResponse.setDescription(job.getDescription());
        jobResponse.setLevel(job.getLevel());
        jobResponse.setStartDate(job.getStartDate());
        jobResponse.setEndDate(job.getEndDate());
        jobResponse.setActive(job.isActive());
        jobResponse.setWorkingType(job.getWorkingType());

        if(job.getSkills() != null){
            jobResponse.setSkills(job.getSkills());
        }

        if(job.getCareer() != null){
            jobResponse.setCareer(job.getCareer());
        }

        if(job.getRecruiter() != null){
            JobResponse.RecruiterJob recruiterJob = new JobResponse.RecruiterJob(
                    job.getRecruiter().getUserId(),
                    job.getRecruiter().getFullName()
            );
            jobResponse.setRecruiter(recruiterJob);
        }

        return jobResponse;
    }

    private List<JobActivateResponse> handleSetActiveForJobs(List<Long> jobIds, boolean activeFlag) {
        if (jobIds == null || jobIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Job> existingJobs = this.jobRepository.findAllById(jobIds);
        Map<Long, Job> jobMap = existingJobs.stream()
                .collect(Collectors.toMap(Job::getJobId, j -> j));

        List<JobActivateResponse> results = new ArrayList<>(jobIds.size());

        for (Long id : jobIds) {
            Job job = jobMap.get(id);
            if (job == null) {
                results.add(new JobActivateResponse(id, false, "fail", "Job not found"));
                continue;
            }

            try {
                // if set active = true, need to check if current date is after end date
                // if true, cannot activate the job
                // if set active = false, can always deactivate
                if (activeFlag) {
                    if (job.getEndDate() != null) {
                        LocalDate currentDate = LocalDate.now();
                        if (currentDate.isAfter(job.getEndDate())) {
                            results.add(new JobActivateResponse(id, job.isActive(), "fail", "End date has passed"));
                            continue;
                        }
                    }
                }
                job.setActive(activeFlag);
                this.jobRepository.save(job);
                results.add(new JobActivateResponse(id, activeFlag, "success", "Successfully updated"));
            } catch (Exception ex) {
                // in case save fails, return current state as false and status fail
                boolean resultingState = job.isActive();
                results.add(new JobActivateResponse(id, resultingState, "fail", "Error updating job"));
            }
        }

        return results;
    }
}

