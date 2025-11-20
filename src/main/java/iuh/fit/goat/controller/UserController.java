package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.request.FollowRecruiterRequest;
import iuh.fit.goat.dto.request.ResetPasswordRequest;
import iuh.fit.goat.dto.request.SaveJobRequest;
import iuh.fit.goat.dto.request.UpdatePasswordRequest;
import iuh.fit.goat.dto.response.LoginResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.UserResponse;
import iuh.fit.goat.entity.Job;
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
        User user =  this.userService.handleGetUserByEmail(email);
        return ResponseEntity.status(HttpStatus.OK).body((T) user);
    }

    @PutMapping("/users/update-password")
    public ResponseEntity<LoginResponse> updatePassword(
            @CookieValue(name = "refreshToken", defaultValue = "missingValue") String refreshToken,
            @Valid @RequestBody UpdatePasswordRequest updatePasswordRequest
    ) throws InvalidException {
        boolean checked = this.userService.handleCheckCurrentPassword(updatePasswordRequest.getCurrentPassword());
        if(!checked) {
            throw new InvalidException("Current password is error");
        }

        Map<String, Object> result = this.userService
                .handleUpdatePassword(updatePasswordRequest.getNewPassword(), refreshToken);
        if(result == null) {
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
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest)
            throws InvalidException {
        try {
            this.userService.handleResetPassword(resetPasswordRequest);
            return ResponseEntity.status(HttpStatus.OK).body(
                    Map.of("message", "Reset password successful")
            );
        } catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/users/me/saved-jobs")
    public ResponseEntity<List<Job>> getCurrentUserSavedJobs() {
        List<Job> savedJobs = this.userService.handleGetCurrentUserSavedJobs();
        return ResponseEntity.status(HttpStatus.OK).body(savedJobs);
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

    @PutMapping("/users/followed-recruiters")
    public ResponseEntity<UserResponse> followRecruiters(@Valid @RequestBody FollowRecruiterRequest followRecruiterRequest)
            throws InvalidException
    {
        User user = this.userService.handleGetUserById(followRecruiterRequest.getUserId());
        if(user == null) {
            throw new InvalidException("User not found");
        }

        UserResponse userResponse = this.userService.handleFollowRecruiters(followRecruiterRequest);
        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }
}
