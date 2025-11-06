package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.EmailJobResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.entity.Subscriber;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public interface SubscriberService {
    Subscriber handleCreateSubscriber(Subscriber subscriber);

    Subscriber handleUpdateSubscriber(Subscriber subscriber);

    void handleDeleteSubscriber(long id);

    Subscriber handleGetSubscriberById(long id);

    ResultPaginationResponse handleGetAllSubscribers(Specification<Subscriber> spec, Pageable pageable);

    Subscriber handleGetSubscribersSkill(String email);

    Subscriber handleGetSubscriberByEmail(String email);

    void handleSendSubscribersEmailJobs();

    void handleSendFollowersEmailJobs();

    boolean isRecentJob(Job job, Instant sevenDaysAgo);

    EmailJobResponse convertJobToSendEmail(Job job);
}
