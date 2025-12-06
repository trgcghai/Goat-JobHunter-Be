package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.request.user.ResetPasswordRequest;
import iuh.fit.goat.dto.request.user.UpdatePasswordRequest;
import iuh.fit.goat.dto.request.user.UserEnabledRequest;
import iuh.fit.goat.dto.response.auth.LoginResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.user.UserEnabledResponse;
import iuh.fit.goat.dto.response.user.UserResponse;
import iuh.fit.goat.entity.Notification;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Value("${minhdat.jwt.access-token-validity-in-seconds}")
    private long jwtAccessToken;
    @Value("${minhdat.jwt.refresh-token-validity-in-seconds}")
    private long jwtRefreshToken;

    @GetMapping("/users")
    public ResponseEntity<ResultPaginationResponse> getAllUsers(
            @Filter Specification<User> spec, Pageable pageable
    ) {
        ResultPaginationResponse result = this.userService.handleGetAllUsers(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PostMapping("/users")
    public <T extends User> ResponseEntity<T> getUserByEmail() {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : null;
        User user = this.userService.handleGetUserByEmail(email);
        return ResponseEntity.status(HttpStatus.OK).body((T) user);
    }

    @PutMapping("/users/update-password")
    public ResponseEntity<LoginResponse> updatePassword(
            @CookieValue(name = "refreshToken", defaultValue = "missingValue") String refreshToken,
            @Valid @RequestBody UpdatePasswordRequest updatePasswordRequest
    ) throws InvalidException {
        boolean checked = this.userService.handleCheckCurrentPassword(updatePasswordRequest.getCurrentPassword());
        if (!checked) {
            throw new InvalidException("Current password is error");
        }

        Map<String, Object> result = this.userService
                .handleUpdatePassword(updatePasswordRequest.getNewPassword(), refreshToken);
        if (result == null) {
            throw new InvalidException("Updated password is failed");
        }

        ResponseCookie accessTokenCookie = ResponseCookie
                .from("accessToken", result.get("accessToken").toString())
                .httpOnly(true)
                .secure(false) // for dev
                .sameSite("Lax") // for dev
                .path("/")
                .maxAge(jwtAccessToken)
                .build();
        ResponseCookie refreshTokenCookie = ResponseCookie
                .from("refreshToken", result.get("refreshToken").toString())
                .httpOnly(true)
                .secure(false) // for dev
                .sameSite("Lax") // for dev
                .path("/")
                .maxAge(jwtRefreshToken)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body((LoginResponse) result.get("loginResponse"));
    }

    @PutMapping("/users/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        try {
            this.userService.handleResetPassword(resetPasswordRequest);
            return ResponseEntity.status(HttpStatus.OK).body(
                    Map.of("message", "Reset password successful")
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/users/me/saved-jobs")
    public ResponseEntity<ResultPaginationResponse> getCurrentUserSavedJobs(Pageable pageable) {
        ResultPaginationResponse result = this.userService.handleGetCurrentUserSavedJobs(pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/users/me/saved-jobs/contains")
    public ResponseEntity<List<Map<String, Object>>> checkJobsSaved(@RequestParam List<Long> jobIds) {
        List<Map<String, Object>> result = this.userService.handleCheckJobsSaved(jobIds);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PutMapping("/users/me/saved-jobs")
    public ResponseEntity<UserResponse> saveJobsForCurrentUser(@RequestBody Map<String, List<Long>> request)
            throws InvalidException {
        List<Long> jobIds = request.get("jobIds");
        if (jobIds == null || jobIds.isEmpty()) {
            throw new InvalidException("Job IDs list cannot be empty");
        }

        UserResponse userResponse = this.userService.handleSaveJobsForCurrentUser(jobIds);
        if (userResponse == null) {
            throw new InvalidException("Failed to save jobs");
        }

        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }

    @DeleteMapping("/users/me/saved-jobs")
    public ResponseEntity<UserResponse> unsaveJobsForCurrentUser(@RequestBody Map<String, List<Long>> request)
            throws InvalidException {
        List<Long> jobIds = request.get("jobIds");
        if (jobIds == null || jobIds.isEmpty()) {
            throw new InvalidException("Job IDs list cannot be empty");
        }

        UserResponse userResponse = this.userService.handleUnsaveJobsForCurrentUser(jobIds);
        if (userResponse == null) {
            throw new InvalidException("Failed to unsave jobs");
        }

        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }

    @GetMapping("/users/me/notifications")
    public ResponseEntity<ResultPaginationResponse> getCurrentUserNotifications(Pageable pageable) {
        ResultPaginationResponse result = this.userService.handleGetCurrentUserNotifications(pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/users/me/notifications/latest")
    public ResponseEntity<List<Notification>> getLatestNotifications() {
        List<Notification> notifications = this.userService.handleGetLatestNotifications();
        return ResponseEntity.status(HttpStatus.OK).body(notifications);
    }

    @PutMapping("/users/me/notifications")
    public ResponseEntity<Map<String, String>> markNotificationsAsSeen(
            @RequestBody List<Long> notificationIds
    ) throws InvalidException {
        if (notificationIds == null) {
            throw new InvalidException("Notification IDs list cannot be null");
        }

        if (notificationIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(
                    Map.of("message", "No notifications to mark as seen")
            );
        }

        this.userService.handleMarkNotificationsAsSeen(notificationIds);

        return ResponseEntity.status(HttpStatus.OK).body(
                Map.of("message", "Notifications marked as seen successfully")
        );
    }

    // endpoints for follow recruiters feature
    @GetMapping("/users/me/followed-recruiters")
    public ResponseEntity<List<Recruiter>> getCurrentUserFollowedRecruiters() {
        List<Recruiter> followed = this.userService.handleGetCurrentUserFollowedRecruiters();
        return ResponseEntity.status(HttpStatus.OK).body(followed);
    }

    @GetMapping("/users/me/followed-recruiters/contains")
    public ResponseEntity<List<Map<String, Object>>> checkRecruitersFollowed(@RequestParam List<Long> recruiterIds) {
        List<Map<String, Object>> result = this.userService.handleCheckRecruitersFollowed(recruiterIds);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PutMapping("/users/me/followed-recruiters")
    public ResponseEntity<UserResponse> followRecruiters(@RequestBody Map<String, List<Long>> request)
            throws InvalidException {
        List<Long> recruiterIds = request.get("recruiterIds");
        if (recruiterIds == null || recruiterIds.isEmpty()) {
            throw new InvalidException("Recruiter IDs list cannot be empty");
        }

        UserResponse userResponse = this.userService.handleFollowRecruiters(recruiterIds);
        if (userResponse == null) {
            throw new InvalidException("Failed to follow recruiters");
        }

        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }

    @DeleteMapping("/users/me/followed-recruiters")
    public ResponseEntity<UserResponse> unfollowRecruiters(@RequestBody Map<String, List<Long>> request)
            throws InvalidException {
        List<Long> recruiterIds = request.get("recruiterIds");
        if (recruiterIds == null || recruiterIds.isEmpty()) {
            throw new InvalidException("Recruiter IDs list cannot be empty");
        }

        UserResponse userResponse = this.userService.handleUnfollowRecruiters(recruiterIds);
        if (userResponse == null) {
            throw new InvalidException("Failed to unfollow recruiters");
        }

        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }

    @PutMapping("/users/activate")
    public ResponseEntity<List<UserEnabledResponse>> activateUsers(@RequestBody UserEnabledRequest request)
            throws InvalidException {
        List<Long> userIds = request.getUserIds();
        if (userIds == null || userIds.isEmpty()) {
            throw new InvalidException("User IDs list cannot be empty");
        }
        List<UserEnabledResponse> res = this.userService.handleActivateUsers(userIds);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PutMapping("/users/deactivate")
    public ResponseEntity<List<UserEnabledResponse>> deactivateUsers(@RequestBody UserEnabledRequest request)
            throws InvalidException {
        List<Long> userIds = request.getUserIds();
        if (userIds == null || userIds.isEmpty()) {
            throw new InvalidException("User IDs list cannot be empty");
        }
        List<UserEnabledResponse> res = this.userService.handleDeactivateUsers(userIds);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }
}
