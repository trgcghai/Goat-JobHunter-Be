package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.subscriber.SubscriberCreateDto;
import iuh.fit.goat.dto.request.subscriber.SubscriberUpdateDto;
import iuh.fit.goat.dto.response.job.EmailJobResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.JobRepository;
import iuh.fit.goat.repository.SkillRepository;
import iuh.fit.goat.repository.SubscriberRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.EmailNotificationService;
import iuh.fit.goat.service.SubscriberService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriberServiceImpl implements SubscriberService {
    private final EmailNotificationService emailNotificationService;

    private final SubscriberRepository subscriberRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    @Override
    public Subscriber handleCreateSubscriber(SubscriberCreateDto dto) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();
        if(currentEmail.isEmpty()) return null;

        User user = this.userRepository.findByEmail(currentEmail);
        Subscriber subscriber = new Subscriber();
        subscriber.setName(user.getFullName());
        subscriber.setEmail(user.getEmail());

        if (dto.getSkillIds() != null && !dto.getSkillIds().isEmpty()) {
            List<Skill> skills = this.skillRepository.findBySkillIdIn(dto.getSkillIds());
            subscriber.setSkills(skills);
        }

        return this.subscriberRepository.save(subscriber);
    }

    @Override
    public Subscriber handleUpdateSubscriber(SubscriberUpdateDto dto) {
        Subscriber currentSubscriber = this.handleGetSubscriberById(dto.getSubscriberId());

        if (dto.getName() != null) {
            currentSubscriber.setName(dto.getName());
        }

        if (dto.getEmail() != null) {
            currentSubscriber.setEmail(dto.getEmail());
        }

        if (dto.getSkillIds() != null) {
            List<Skill> skills = this.skillRepository.findBySkillIdIn(dto.getSkillIds());
            currentSubscriber.setSkills(skills);
        }

        return this.subscriberRepository.save(currentSubscriber);
    }

    @Override
    public void handleDeleteSubscriber(long id) {
        this.jobRepository.deleteById(id);
    }

    @Override
    public Subscriber handleGetSubscriberById(long id) {
        Optional<Subscriber> optional = this.subscriberRepository.findById(id);
        return optional.orElse(null);
    }

    @Override
    public ResultPaginationResponse handleGetAllSubscribers(Specification<Subscriber> spec, Pageable pageable) {
        Page<Subscriber> page = this.subscriberRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        return new ResultPaginationResponse(meta, page.getContent());
    }

    @Override
    public Subscriber handleGetSubscribersSkill(String email) {
        return this.subscriberRepository.findByEmail(email).orElse(null);
    }

    @Override
    public Subscriber handleGetSubscriberByEmail() {
        String email = SecurityUtil.getCurrentUserEmail();
        return this.subscriberRepository.findByEmail(email).orElse(null);
    }

    @Override
    public void handleSendSubscribersEmailJobs() {
        List<Subscriber> listSubs = this.subscriberRepository.findAll();
        if (!listSubs.isEmpty()) {
            for (Subscriber sub : listSubs) {
                List<Skill> listSkills = sub.getSkills();
                if (listSkills != null && !listSkills.isEmpty()) {
                    List<Job> listJobs = this.jobRepository.findBySkillsIn(listSkills);
                    if (listJobs != null && !listJobs.isEmpty()) {
                        List<EmailJobResponse> arr = listJobs.stream().map(
                                this::convertJobToSendEmail).collect(Collectors.toList()
                        );
                        this.emailNotificationService.handleSendEmailWithTemplate(
                                sub.getEmail(),
                                "Cơ hội việc làm hot đang chờ đón bạn, khám phá ngay",
                                "job",
                                sub.getName(),
                                arr);
                    }
                }
            }
        }
    }

    @Override
    public void handleSendFollowersEmailJobs() {
        List<User> users = this.userRepository.findAll();
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        if(users.isEmpty()) return;

        for(User user : users) {
            List<Company> companies = user.getFollowedCompanies();
            if(companies == null || companies.isEmpty()) continue;

            List<Job> allRecentJobs = new ArrayList<>();

            for(Company company : companies) {
                List<Job> jobs = this.jobRepository.findByCompany(company);
                if(jobs == null || jobs.isEmpty()) continue;

                List<Job> recentJobs = jobs.stream().filter(job -> isRecentJob(job, sevenDaysAgo)).toList();
                allRecentJobs.addAll(recentJobs);
            }

            if(!allRecentJobs.isEmpty()) {
                List<EmailJobResponse> arr = allRecentJobs.stream().map(
                                this::convertJobToSendEmail)
                        .collect(Collectors.toList());
                this.emailNotificationService.handleSendEmailWithTemplate(
                        user.getEmail(),
                        "Cơ hội việc làm hot đang chờ đón bạn, khám phá ngay",
                        "job",
                        user.getFullName(),
                        arr);
            }
        }
    }

    @Override
    public boolean isRecentJob(Job job, Instant sevenDaysAgo) {
        Instant updatedAt = job.getUpdatedAt();
        Instant createdAt = job.getCreatedAt();

        return (updatedAt != null && updatedAt.isAfter(sevenDaysAgo)) ||
                (updatedAt == null && createdAt != null && createdAt.isAfter(sevenDaysAgo));
    }

    @Override
    public EmailJobResponse convertJobToSendEmail(Job job) {
        EmailJobResponse res = new EmailJobResponse();
        res.setTitle(job.getTitle());
        res.setSalary(job.getSalary());
        res.setCompany(new EmailJobResponse.CompanyEmail(job.getCompany().getName()));
        List<Skill> skills = job.getSkills();
        List<EmailJobResponse.SkillEmail> skillResponses = skills.stream()
                .map(skill -> new EmailJobResponse.SkillEmail(skill.getName()))
                .toList();
        res.setSkills(skillResponses);

        return res;
    }
}
