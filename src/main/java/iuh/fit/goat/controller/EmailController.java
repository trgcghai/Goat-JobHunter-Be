package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.JobInvitationRequest;
import iuh.fit.goat.service.EmailService;
import iuh.fit.goat.service.SubscriberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EmailController {
    private final SubscriberService subscriberService;
    private final EmailService emailService;

    @GetMapping("/email")
//    @Scheduled(cron = "*/10 * * * * *")
//    @Transactional
    public void sendEmail(){
        this.subscriberService.handleSendSubscribersEmailJobs();
        this.subscriberService.handleSendFollowersEmailJobs();
    }

    @GetMapping("email/jobs")
    public void sendJobInvitation(@Valid @RequestBody JobInvitationRequest request) {
        this.emailService.handelSendJobInvitationEmail(request.getApplicantIds(), request.getJobId());
    }

}