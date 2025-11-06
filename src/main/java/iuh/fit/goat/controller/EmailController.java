package iuh.fit.goat.controller;

import iuh.fit.goat.service.SubscriberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EmailController {
    private final SubscriberService subscriberService;

    @GetMapping("/email")
    public void sendEmail(){
        this.subscriberService.handleSendSubscribersEmailJobs();
        this.subscriberService.handleSendFollowersEmailJobs();
    }

}