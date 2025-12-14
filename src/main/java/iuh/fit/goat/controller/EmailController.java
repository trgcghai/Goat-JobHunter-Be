package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.job.JobInvitationRequest;
import iuh.fit.goat.service.EmailNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EmailController {
//    private final EmailNotificationService emailNotificationService;
//
//    @PostMapping("email/jobs")
//    public void sendJobInvitation(@Valid @RequestBody JobInvitationRequest request) {
//        this.emailNotificationService.handleSendJobInvitationEmail(request.getApplicantIds(), request.getJobId());
//    }
}