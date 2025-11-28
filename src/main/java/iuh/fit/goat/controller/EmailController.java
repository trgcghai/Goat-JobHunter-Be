package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.job.JobInvitationRequest;
import iuh.fit.goat.service.EmailNotificationService;
import iuh.fit.goat.service.SubscriberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EmailController {
    private final SubscriberService subscriberService;
    private final EmailNotificationService emailNotificationService;

    @GetMapping("/email")
//    @Scheduled(cron = "*/10 * * * * *")
//    @Transactional
    public void sendEmail(){
        this.subscriberService.handleSendSubscribersEmailJobs();
        this.subscriberService.handleSendFollowersEmailJobs();
    }

    @PostMapping("email/jobs")
    public void sendJobInvitation(@Valid @RequestBody JobInvitationRequest request) {
        this.emailNotificationService.handleSendJobInvitationEmail(request.getApplicantIds(), request.getJobId());
    }

}