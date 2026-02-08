package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.ActionType;
import iuh.fit.goat.dto.response.job.*;
import iuh.fit.goat.enumeration.Level;
import iuh.fit.goat.common.Role;
import iuh.fit.goat.enumeration.WorkingType;
import iuh.fit.goat.dto.request.job.CreateJobRequest;
import iuh.fit.goat.dto.request.job.JobIdsActionRequest;
import iuh.fit.goat.dto.request.job.UpdateJobRequest;
import iuh.fit.goat.dto.response.applicant.ApplicantResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.service.*;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.util.SecurityUtil;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {
    private final AccountService accountService;
    private final ApplicantService applicantService;
    private final EmailNotificationService emailNotificationService;

    private final CompanyRepository companyRepository;
    private final ApplicantRepository applicantRepository;
    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final CareerRepository careerRepository;
    private final ApplicationRepository applicationRepository;
    private final SubscriberRepository subscriberRepository;

    @Override
    public JobResponse handleCreateJob(CreateJobRequest request) {
        Job job = new Job();
        job.setTitle(request.getTitle());
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

        if (request.getCompanyId() != null) {
            Optional<Company> company = this.companyRepository.findById(request.getCompanyId());
            company.ifPresent(job::setCompany);
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

        Job res = this.jobRepository.save(currentJob);

        JobResponse jobResponse = convertToJobResponse(res);
        jobResponse.setUpdatedAt(res.getUpdatedAt());
        jobResponse.setUpdatedBy(res.getUpdatedBy());

        return jobResponse;
    }

    @Override
    public void handleDeleteJob(JobIdsActionRequest request) {
        List<Job> jobs = this.jobRepository.findAllById(request.getJobIds());
        if(jobs.isEmpty()) return;

        String currentEmail = SecurityUtil.getCurrentUserEmail();
        Account currentAccount = this.accountService.handleGetAccountByEmail(currentEmail);
        if(currentAccount == null) return;

        if(!currentAccount.isEnabled() || !currentAccount.getRole().isActive()) return;

        this.jobRepository.deleteAllById(request.getJobIds());

        if(currentAccount.getRole().getName().equalsIgnoreCase(Role.ADMIN.getValue())) {
            Map<String, List<Job>> jobByEmail = jobs.stream()
                    .collect(Collectors.groupingBy(job -> job.getCompany().getEmail()));

            jobByEmail.forEach((email, js) -> {
                if(js.isEmpty()) return;

                this.emailNotificationService.handleSendJobActionNotice(
                        email, js.getFirst().getCompany().getUsername(),
                        js, request.getReason(), ActionType.DELETE
                );
            });
        }

    }

    @Override
    public Job handleGetJobById(long id) {
        return this.jobRepository.findByJobIdAndDeletedAtIsNull(id).orElse(null);
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
    public Map<Long, Long> handleCountAvailableJobByCompany(){
        return this.jobRepository.countAvailableJobs()
                .stream().collect(
                        Collectors.toMap(
                                row-> (Long)row[0],
                                row-> (Long)row[1]
                        )
                );
    }

    @Override
    public List<Long> handleGetAllJobIdsByCompany() {
        List<Long> jobIds = null;

        String email = SecurityUtil.getCurrentUserEmail();
        Company company = this.companyRepository.findByEmail(email).orElse(null);
        if(company != null){
            List<Job> jobs = company.getJobs();
            if(jobs != null && !jobs.isEmpty()){
                jobIds = jobs.stream().map(Job::getJobId).toList();
            }
        }

        return jobIds;
    }

    @Override
    public ResultPaginationResponse handleGetCurrentCompanyJobs(Specification<Job> spec, Pageable pageable) {
        String email = SecurityUtil.getCurrentUserEmail();
        if (email.isEmpty()) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }
        Company currentCompany = this.companyRepository.findByEmail(email).orElse(null);
        if (currentCompany == null) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }
        Long companyId = currentCompany.getAccountId();
        return this.handleGetJobsByCompanyId(companyId, spec, pageable);
    }

    @Override
    public ResultPaginationResponse handleGetJobsByCompanyId(Long companyId, Specification<Job> spec, Pageable pageable) {
        if (companyId == null) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        Specification<Job> companySpec = (root, query, criteriaBuilder) ->
                criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("company").get("accountId"), companyId),
                        criteriaBuilder.isNull(root.get("deletedAt"))
                );

        Specification<Job> finalSpec = (spec != null) ? spec.and(companySpec) : companySpec;

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
    public List<JobResponse> handleGetAllAvailableJobsByCompanyId(Long companyId, Specification<Job> spec) {
        Specification<Job> companySpec = ((root, query, cb) ->
                cb.and(
                    cb.equal(root.get("company").get("accountId"), companyId),
                    cb.isTrue(root.get("enabled")),
                    cb.isNull(root.get("deletedAt"))
                )
        );

        Specification<Job> finalSpec = (spec != null) ? spec.and(companySpec) : companySpec;

        List<Job> jobs = this.jobRepository.findAll(finalSpec);

        return jobs.stream()
                .map(this::convertToJobResponse)
                .toList();
    }

    @Override
    public List<JobActivateResponse> handleActivateJobs(List<Long> jobIds) {
        return this.handleSetActiveForJobs(jobIds, true);
    }

    @Override
    public List<JobActivateResponse> handleDeactivateJobs(List<Long> jobIds) {
        return this.handleSetActiveForJobs(jobIds, false);
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

            List<Application> apps = this.applicationRepository.findByJobAndDeletedAtIsNull(job);
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

                    Subscriber subscriber = emailSubscriberMap.get(a.getEmail());
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
    public ResultPaginationResponse handleGetApplicants(Specification<Applicant> spec, Pageable pageable) {
        Page<Applicant> page = this.applicantRepository.findAll(spec, pageable);

        List<Applicant> availableApplicants = page.getContent().stream()
                .filter(Applicant::isAvailableStatus)
                .toList();

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<ApplicantResponse> result = availableApplicants.stream()
                .map(this.applicantService::convertToApplicantResponse)
                .toList();

        return new ResultPaginationResponse(meta, result);
    }


    @Override
    @Transactional
    public List<JobEnabledResponse> handleEnabledJobs(JobIdsActionRequest request) {
        List<Job> jobs = this.jobRepository.findAllById(request.getJobIds());
        if(jobs.isEmpty()) return Collections.emptyList();

        jobs.forEach(job -> job.setEnabled(true));
        this.jobRepository.saveAll(jobs);

        Map<String, List<Job>> jobByEmail =
                jobs.stream().collect(Collectors.groupingBy(job -> job.getCompany().getEmail()));

        jobByEmail.forEach((email, js) -> {
            if(js.isEmpty()) return;

            this.emailNotificationService.handleSendJobActionNotice(
                    email, js.getFirst().getCompany().getUsername(),
                    js, null, ActionType.ACCEPT
            );
        });

        return jobs.stream().map(
                job -> new JobEnabledResponse(
                        job.getJobId(),
                        job.isEnabled()
                )
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<JobEnabledResponse> handleDisabledJobs(JobIdsActionRequest request) {
        List<Job> jobs = this.jobRepository.findAllById(request.getJobIds());
        if(jobs.isEmpty()) return Collections.emptyList();

        jobs.forEach(job -> job.setEnabled(false));
        this.jobRepository.saveAll(jobs);

        Map<String, List<Job>> jobByEmail =
                jobs.stream().collect(Collectors.groupingBy(job -> job.getCompany().getEmail()));

        jobByEmail.forEach((email, js) -> {
            if(js.isEmpty()) return;
            this.emailNotificationService.handleSendJobActionNotice(
                    email, js.getFirst().getCompany().getUsername(),
                    js, request.getReason(), ActionType.REJECT
            );
        });

        return jobs.stream().map(
                job -> new JobEnabledResponse(
                        job.getJobId(),
                        job.isEnabled()
                )
        ).collect(Collectors.toList());
    }

    @Override
    public ResultPaginationResponse handleGetJobSubscribersByCurrentUser(
            Specification<Job> spec, Pageable pageable
    ) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();
        if(currentEmail.isEmpty()) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        Subscriber subscriber = this.subscriberRepository.findByEmail(currentEmail).orElse(null);
        if(subscriber == null) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        List<Skill> skills = subscriber.getSkills();
        if(skills == null || skills.isEmpty()) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        Specification<Job> finalSpec = spec.and((root, query, cb) -> {
            assert query != null;
            query.distinct(true);

            Predicate enabledPredicate = cb.isTrue(root.get("enabled"));
            Predicate notDeletedPredicate = cb.isNull(root.get("deletedAt"));

            Join<Job, Skill> skillJoin = root.join("skills", JoinType.INNER);
            Predicate skillPredicate = skillJoin.in(skills);

            return cb.and(enabledPredicate, notDeletedPredicate, skillPredicate);
        });

        Page<Job> page = this.jobRepository.findAll(finalSpec, pageable);
        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        return new ResultPaginationResponse(meta,
                page.getContent().stream()
                        .map(this::convertToJobResponse)
                        .toList()
        );
    }

    @Override
    public ResultPaginationResponse handleGetRelatedJobsByCurrentUser(Specification<Job> spec, Pageable pageable) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();
        if(currentEmail.isEmpty()) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }
        Account user = this.accountService.handleGetAccountByEmail(currentEmail);

        List<Long> relatedJobIds = this.jobRepository.findRelatedJobsByCurrentUser(user.getAccountId());
        if (relatedJobIds.isEmpty()) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        Specification<Job> finalSpec = spec.and((root, query, cb) -> root.get("jobId").in(relatedJobIds));

        Page<Job> page = this.jobRepository.findAll(finalSpec, pageable);
        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        return new ResultPaginationResponse(
                meta,
                page.getContent().stream()
                        .map(this::convertToJobResponse)
                        .toList()
        );
    }

    @Override
    public JobResponse convertToJobResponse(Job job) {
        JobResponse jobResponse = new JobResponse();

        jobResponse.setJobId(job.getJobId());
        jobResponse.setTitle(job.getTitle());
        jobResponse.setSalary(job.getSalary());
        jobResponse.setQuantity(job.getQuantity());
        jobResponse.setDescription(job.getDescription());
        jobResponse.setLevel(job.getLevel());
        jobResponse.setStartDate(job.getStartDate());
        jobResponse.setEndDate(job.getEndDate());
        jobResponse.setActive(job.isActive());
        jobResponse.setEnabled(job.isEnabled());
        jobResponse.setWorkingType(job.getWorkingType());

        if(job.getSkills() != null){
            jobResponse.setSkills(job.getSkills());
        }

        if(job.getCareer() != null){
            jobResponse.setCareer(job.getCareer());
        }

        if(job.getCompany() != null){
            JobResponse.JobCompany jobCompany = new JobResponse.JobCompany(
                    job.getCompany().getAccountId(),
                    job.getCompany().getName()
            );
            jobResponse.setCompany(jobCompany);
        }

        if(job.getAddress() != null){
            JobResponse.JobAddress jobAddress = new JobResponse.JobAddress(
                    job.getAddress().getAddressId(),
                    job.getAddress().getProvince(),
                    job.getAddress().getFullAddress()
            );
            jobResponse.setAddress(jobAddress);
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

