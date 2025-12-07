package iuh.fit.goat.service.impl;

import iuh.fit.goat.entity.Job;
import iuh.fit.goat.repository.JobRepository;
import iuh.fit.goat.service.ScheduledService;
import iuh.fit.goat.service.SubscriberService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduledServiceImpl implements ScheduledService {
    private final SubscriberService subscriberService;
    private final JobRepository jobRepository;

    @Override
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void handleDeactivateExpiredJobs() {
        LocalDate today = LocalDate.now();
        List<Job> jobs = this.jobRepository.findAll();

        jobs.forEach(job -> {
            if(job.getEndDate() != null && job.getEndDate().isBefore(today)){
                job.setActive(false);
            }
        });

        this.jobRepository.saveAll(jobs);

    }

    @Override
    @Scheduled(cron = "0 0 */12 * * *")
    @Transactional
    public void sendEmail(){
        this.subscriberService.handleSendSubscribersEmailJobs();
        this.subscriberService.handleSendFollowersEmailJobs();
    }
}
