package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.EmailJobResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.JobRepository;
import iuh.fit.goat.repository.SkillRepository;
import iuh.fit.goat.repository.SubscriberRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.EmailService;
import iuh.fit.goat.service.SubscriberService;
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
    private final SubscriberRepository subscriberRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    public Subscriber handleCreateSubscriber(Subscriber subscriber) {
        if (subscriber.getSkills() != null) {
            List<Long> idSkills = subscriber.getSkills().stream().map(Skill::getSkillId)
                    .collect(Collectors.toList());
            List<Skill> skills = this.skillRepository.findBySkillIdIn(idSkills);
            subscriber.setSkills(skills);
        }
        return this.subscriberRepository.save(subscriber);
    }

    @Override
    public Subscriber handleUpdateSubscriber(Subscriber subscriber) {
        Subscriber currentSubscriber = this.handleGetSubscriberById(subscriber.getSubscriberId());

        if (subscriber.getSkills() != null) {
            List<Long> idSkills = subscriber.getSkills().stream().map(Skill::getSkillId)
                    .collect(Collectors.toList());
            List<Skill> skills = this.skillRepository.findBySkillIdIn(idSkills);
            subscriber.setSkills(skills);
        }
        currentSubscriber.setSkills(subscriber.getSkills());

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
        return this.subscriberRepository.findByEmail(email);
    }

    @Override
    public Subscriber handleGetSubscriberByEmail(String email) {
        return this.subscriberRepository.findByEmail(email);
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
                        this.emailService.handleSendEmailWithTemplate(
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
            List<Recruiter> recruiters = user.getFollowedRecruiters();
            if(recruiters == null || recruiters.isEmpty()) continue;

            List<Job> allRecentJobs = new ArrayList<>();

            for(Recruiter recruiter : recruiters) {
                List<Job> jobs = this.jobRepository.findByRecruiter(recruiter);
                if(jobs == null || jobs.isEmpty()) continue;

                List<Job> recentJobs = jobs.stream().filter(job -> isRecentJob(job, sevenDaysAgo)).toList();
                allRecentJobs.addAll(recentJobs);
            }

            if(!allRecentJobs.isEmpty()) {
                List<EmailJobResponse> arr = allRecentJobs.stream().map(
                                this::convertJobToSendEmail)
                        .collect(Collectors.toList());
                this.emailService.handleSendEmailWithTemplate(
                        user.getContact().getEmail(),
                        "Cơ hội việc làm hot đang chờ đón bạn, khám phá ngay",
                        "job",
                        user.getFullName(),
                        arr);
            }
        }
    }

    @Override
    public EmailJobResponse convertJobToSendEmail(Job job) {
        EmailJobResponse res = new EmailJobResponse();
        res.setTitle(job.getTitle());
        res.setSalary(job.getSalary());
        res.setRecruiter(new EmailJobResponse.RecruiterEmail(job.getRecruiter().getFullName()));
        List<Skill> skills = job.getSkills();
        List<EmailJobResponse.SkillEmail> skillResponses = skills.stream()
                .map(skill -> new EmailJobResponse.SkillEmail(skill.getName()))
                .toList();
        res.setSkills(skillResponses);

        return res;
    }
}
