package iuh.fit.goat.controller;


import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Subscriber;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.SubscriberService;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubscriberController {
    private final SubscriberService subscriberService;

    @PostMapping("/subscribers")
    public ResponseEntity<Subscriber> createSubscriber(@Valid @RequestBody Subscriber subscriber)
            throws InvalidException {
        if (this.subscriberService.handleGetSubscriberByEmail(subscriber.getEmail()) == null) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(this.subscriberService.handleCreateSubscriber(subscriber));
        } else {
            throw new InvalidException("Email exists");
        }
    }

    @PutMapping("/subscribers")
    public ResponseEntity<Subscriber> updateSubscriber(@RequestBody Subscriber subscriber)
            throws InvalidException {
        if (this.subscriberService.handleGetSubscriberById(subscriber.getSubscriberId()) != null) {
            return ResponseEntity.ok()
                    .body(this.subscriberService.handleUpdateSubscriber(subscriber));
        } else {
            throw new InvalidException("Subscriber doesn't exist");
        }
    }

    @DeleteMapping("/subscribers/{id}")
    public ResponseEntity<Void> deleteSubscriber(@PathVariable("id") String id)
            throws InvalidException {
        Pattern pattern = Pattern.compile("^[0-9]+$");

        if(pattern.matcher(id).matches()) {
            Subscriber subscriber = this.subscriberService.handleGetSubscriberById(Long.parseLong(id));
            if(subscriber != null) {
                this.subscriberService.handleDeleteSubscriber(Long.parseLong(id));
                return ResponseEntity.status(HttpStatus.OK).body(null);
            } else {
                throw new InvalidException("Subscriber doesn't exist");
            }
        } else {
            throw new InvalidException("Id is number");
        }
    }

    @GetMapping("/subscribers/{id}")
    public ResponseEntity<Subscriber> getSubscriberById(@PathVariable("id") String id) throws InvalidException {
        Pattern pattern = Pattern.compile("^[0-9]+$");

        if(pattern.matcher(id).matches()) {
            Subscriber subscriber = this.subscriberService.handleGetSubscriberById(Long.parseLong(id));
            if(subscriber != null) {
                return ResponseEntity.status(HttpStatus.OK).body(subscriber);
            } else {
                throw new InvalidException("Subscriber doesn't exist");
            }
        } else {
            throw new InvalidException("Id is number");
        }
    }

    @GetMapping("/subscribers")
    public ResponseEntity<ResultPaginationResponse> getAllSubscribers(
            @Filter Specification<Subscriber> spec, Pageable pageable
    ) {
        ResultPaginationResponse result = this.subscriberService.handleGetAllSubscribers(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PostMapping("/subscribers/skills")
    public ResponseEntity<Subscriber> getSubscribersSkill() {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        return ResponseEntity.ok().body(this.subscriberService.handleGetSubscribersSkill(email));
    }
}
