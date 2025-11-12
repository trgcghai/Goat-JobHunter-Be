package iuh.fit.goat.service.impl;

import iuh.fit.goat.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import iuh.fit.goat.dto.response.JobResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.util.SecurityUtil;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {
    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final CareerRepository careerRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Override
    public JobResponse handleCreateJob(Job job) {
        if(job.getSkills() != null){
            List<Long> skillIds = job.getSkills().stream().map(Skill::getSkillId).toList();
            List<Skill> skills = this.skillRepository.findBySkillIdIn(skillIds);
            job.setSkills(skills);
        }
        if(job.getRecruiter() != null){
            Optional<User> currentUser = this.userRepository.findById(job.getRecruiter().getUserId());
            if(currentUser.isPresent() && currentUser.get() instanceof Recruiter recruiter){
                job.setRecruiter(recruiter);
            }
        }
        if(job.getCareer() != null){
            Optional<Career> currentCareer = this.careerRepository.findById(job.getCareer().getCareerId());
            currentCareer.ifPresent(job::setCareer);
        }
        Job res = this.jobRepository.save(job);

        JobResponse jobResponse = convertToJobResponse(res);
        jobResponse.setCreatedAt(res.getCreatedAt());
        jobResponse.setCreatedBy(res.getCreatedBy());

        return jobResponse;
    }

    @Override
    public JobResponse handleUpdateJob(Job job) {
        Job currentJob = this.handleGetJobById(job.getJobId());

        if(job.getSkills() != null){
            List<Long> skillIds = job.getSkills().stream().map(Skill::getSkillId).toList();
            List<Skill> skills = this.skillRepository.findBySkillIdIn(skillIds);
            currentJob.setSkills(skills);
        }
        if(job.getRecruiter() != null){
            Optional<User> currentUser = this.userRepository.findById(job.getRecruiter().getUserId());
            if(currentUser.isPresent() && currentUser.get() instanceof Recruiter recruiter){
                currentJob.setRecruiter(recruiter);
            }
        }
        if(job.getCareer() != null){
            Optional<Career> currentCareer = this.careerRepository.findById(job.getCareer().getCareerId());
            currentCareer.ifPresent(currentJob::setCareer);
        }
        currentJob.setDescription(job.getDescription());
        currentJob.setStartDate(job.getStartDate());
        currentJob.setEndDate(job.getEndDate());
        currentJob.setActive(job.isActive());
        currentJob.setLevel(job.getLevel());
        currentJob.setQuantity(job.getQuantity());
        currentJob.setSalary(job.getSalary());
        currentJob.setTitle(job.getTitle());
        currentJob.setWorkingType(job.getWorkingType());
        currentJob.setLocation(job.getLocation());
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
    public JobResponse convertToJobResponse(Job job) {
        JobResponse jobResponse = new JobResponse();

        jobResponse.setJobId(job.getJobId());
        jobResponse.setTitle(job.getTitle());
        jobResponse.setLocation(job.getLocation());
        jobResponse.setSalary(job.getSalary());
        jobResponse.setQuantity(job.getQuantity());
        jobResponse.setLevel(job.getLevel());
        jobResponse.setStartDate(job.getStartDate());
        jobResponse.setEndDate(job.getEndDate());
        jobResponse.setActive(job.isActive());
        jobResponse.setWorkingType(job.getWorkingType());

        if(job.getSkills() != null){
            List<String> skillStr = job.getSkills().stream().map(Skill::getName).toList();
            jobResponse.setSkills(skillStr);
        }

        if(job.getCareer() != null){
            jobResponse.setCareer(job.getCareer());
        }

        return jobResponse;
    }
}

