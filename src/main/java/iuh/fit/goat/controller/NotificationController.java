package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.notification.NotificationIdsRequest;
import iuh.fit.goat.entity.Notification;
import iuh.fit.goat.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
//    private final NotificationService notificationService;
//
//    @GetMapping
//    public ResponseEntity<List<Notification>> getAllNotifications() {
//        List<Notification> result = this.notificationService.handleGetAllNotifications();
//        return ResponseEntity.ok(result);
//    }
//
//    @PutMapping
//    public ResponseEntity<Map<String, String>> markNotificationsAsSeen(
//            @Valid @RequestBody NotificationIdsRequest request
//    ) {
//        this.notificationService.handleMarkNotificationsAsSeen(request.getNotificationIds());
//        return ResponseEntity.status(HttpStatus.OK).body(
//                Map.of("message", "Notifications marked as seen successfully")
//        );
//    }
}