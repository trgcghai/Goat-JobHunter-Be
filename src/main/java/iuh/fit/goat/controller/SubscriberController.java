package iuh.fit.goat.controller;


import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.request.subscriber.SubscriberCreateDto;
import iuh.fit.goat.dto.request.subscriber.SubscriberUpdateDto;
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
@RequestMapping("/api/v1/subscribers")
@RequiredArgsConstructor
public class SubscriberController {
    private final SubscriberService subscriberService;

    @PostMapping
    public ResponseEntity<Subscriber> createSubscriber(
            @Valid @RequestBody SubscriberCreateDto dto
    ) throws InvalidException
    {
        Subscriber subscriber = this.subscriberService.handleGetSubscriberByEmail();
        if(subscriber != null) throw new InvalidException("Email already exists");

        Subscriber createdSubscriber = this.subscriberService.handleCreateSubscriber(dto);
        if(createdSubscriber == null) throw new InvalidException("Cannot create subscriber");

        return ResponseEntity.status(HttpStatus.CREATED).body(createdSubscriber);
    }

    @PutMapping
    public ResponseEntity<Subscriber> updateSubscriber(@Valid @RequestBody SubscriberUpdateDto dto)
            throws InvalidException {
        if (this.subscriberService.handleGetSubscriberById(dto.getSubscriberId()) != null) {
            return ResponseEntity.ok()
                    .body(this.subscriberService.handleUpdateSubscriber(dto));
        } else {
            throw new InvalidException("Subscriber doesn't exist");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscriber(@PathVariable("id") String id)
            throws InvalidException {
        if(SecurityUtil.checkValidNumber("id")) {
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

    @GetMapping("/{id}")
    public ResponseEntity<Subscriber> getSubscriberById(@PathVariable("id") String id) throws InvalidException {
        if(SecurityUtil.checkValidNumber("id")) {
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

    @GetMapping
    public ResponseEntity<ResultPaginationResponse> getAllSubscribers(
            @Filter Specification<Subscriber> spec, Pageable pageable
    ) {
        ResultPaginationResponse result = this.subscriberService.handleGetAllSubscribers(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/skills")
    public ResponseEntity<Subscriber> getCurrentUserSubscribersSkill() {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        return ResponseEntity.ok().body(this.subscriberService.handleGetSubscribersSkill(email));
    }
}
