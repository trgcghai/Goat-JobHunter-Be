package iuh.fit.goat.service.impl;

import iuh.fit.goat.enumeration.Role;
import iuh.fit.goat.dto.request.user.ResetPasswordRequest;
import iuh.fit.goat.dto.response.auth.LoginResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.user.UserEnabledResponse;
import iuh.fit.goat.dto.response.user.UserResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.JobRepository;
import iuh.fit.goat.repository.NotificationRepository;
import iuh.fit.goat.repository.RecruiterRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.EmailNotificationService;
import iuh.fit.goat.service.NotificationService;
import iuh.fit.goat.service.RedisService;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final RedisService redisService;
    private final NotificationService notificationService;
    private final EmailNotificationService emailNotificationService;

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final RecruiterRepository recruiterRepository;
    private final NotificationRepository notificationRepository;

    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;

    @Value("${minhdat.jwt.refresh-token-validity-in-seconds}")
    private long jwtRefreshToken;
    @Value("${minhdat.verify-code-validity-in-seconds}")
    private long validityInSeconds;

    @Override
    public User handleGetUserByEmail(String email) {
        return this.userRepository.findByContact_Email(email);
    }

    @Override
    public boolean handleExistsByEmail(String email) {
        return this.userRepository.existsByContact_Email(email);
    }

    @Override
    public User handleGetUserById(long id) {
        return this.userRepository.findById(id).orElse(null);
    }

    @Override
    public ResultPaginationResponse handleGetAllUsers(Specification<User> spec, Pageable pageable) {
        Page<User> page = this.userRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<UserResponse> userResponses = page.getContent().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());

        return new ResultPaginationResponse(meta, userResponses);
    }

    @Override
    public boolean handleCheckCurrentPassword(String currentPassword) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if (!currentEmail.isEmpty()) {
            User currentUser = this.handleGetUserByEmail(currentEmail);
            return passwordEncoder.matches(currentPassword, currentUser.getPassword());
        }

        return false;
    }

    @Override
    public Map<String, Object> handleUpdatePassword(String newPassword, String refreshToken) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if (!currentEmail.isEmpty()) {
            User currentUser = this.handleGetUserByEmail(currentEmail);
            String hashedPassword = this.passwordEncoder.encode(newPassword);
            currentUser.setPassword(hashedPassword);
            User res = this.userRepository.save(currentUser);

            LoginResponse loginResponse = new LoginResponse();
            LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin();

            userLogin.setUserId(res.getUserId());
            userLogin.setDob(res.getDob());
            userLogin.setGender(res.getGender());
            userLogin.setFullName(res.getFullName());
            userLogin.setUsername(res.getUsername());
            userLogin.setContact(res.getContact());
            userLogin.setAvatar(res.getAvatar());
            userLogin.setType(res instanceof Applicant ? Role.APPLICANT.getValue() : Role.RECRUITER.getValue());
            userLogin.setRole(res.getRole());
            userLogin.setEnabled(res.isEnabled());

            loginResponse.setUser(userLogin);
            String newAccessToken = this.securityUtil.createAccessToken(currentEmail, loginResponse);
            String newRefreshToken = this.securityUtil.createRefreshToken(currentEmail, loginResponse);
            this.redisService.replaceKey(
                    "refresh:" + refreshToken,
                    "refresh:" + newRefreshToken,
                    currentEmail, jwtRefreshToken, TimeUnit.SECONDS

            );

            Map<String, Object> response = new HashMap<>();
            response.put("loginResponse", loginResponse);
            response.put("refreshToken", newRefreshToken);
            response.put("accessToken", newAccessToken);

            return response;
        }

        return null;
    }

    @Override
    public void handleResetPassword(ResetPasswordRequest resetPasswordRequest) throws InvalidException {
        User user = this.handleGetUserByEmail(resetPasswordRequest.getEmail());
        if (user != null) {
            String hashedPassword = this.passwordEncoder.encode(resetPasswordRequest.getNewPassword());
            user.setPassword(hashedPassword);
            user.setEnabled(false);
            this.userRepository.save(user);

            String verificationCode = SecurityUtil.generateVerificationCode();
            this.redisService.saveWithTTL(
                    user.getContact().getEmail(),
                    verificationCode,
                    validityInSeconds,
                    TimeUnit.SECONDS
            );
            this.emailNotificationService.handleSendVerificationEmail(user.getContact().getEmail(), verificationCode);
        } else {
            throw new InvalidException("User not found");
        }
    }

    @Override
    public ResultPaginationResponse handleGetCurrentUserSavedJobs(Pageable pageable) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if (currentEmail.isEmpty()) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null || currentUser.getSavedJobs() == null) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        List<Job> savedJobs = currentUser.getSavedJobs();
        int total = savedJobs.size();
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int start = pageNumber * pageSize;
        List<Job> content;

        if (start >= total || pageSize <= 0) {
            content = new ArrayList<>();
        } else {
            int end = Math.min(start + pageSize, total);
            content = savedJobs.subList(start, end);
        }

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageNumber + 1);
        meta.setPageSize(pageSize);
        int pages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
        meta.setPages(pages);
        meta.setTotal((long) total);

        return new ResultPaginationResponse(meta, content);
    }

    @Override
    public List<Map<String, Object>> handleCheckJobsSaved(List<Long> jobIds) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if (currentEmail.isEmpty()) {
            return new ArrayList<>();
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        List<Long> savedJobIds = currentUser.getSavedJobs() != null
                ? currentUser.getSavedJobs().stream().map(Job::getJobId).toList()
                : new ArrayList<>();

        return jobIds.stream()
                .map(jobId -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("jobId", jobId);
                    result.put("result", savedJobIds.contains(jobId));
                    return result;
                })
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse handleSaveJobsForCurrentUser(List<Long> jobIds) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if (currentEmail.isEmpty()) {
            return null;
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null) {
            return null;
        }

        List<Job> currentSavedJobs = currentUser.getSavedJobs() != null
                ? new ArrayList<>(currentUser.getSavedJobs())
                : new ArrayList<>();

        List<Job> jobsToAdd = this.jobRepository.findByJobIdIn(jobIds);

        for (Job job : jobsToAdd) {
            if (currentSavedJobs.stream().noneMatch(j -> j.getJobId() == job.getJobId())) {
                currentSavedJobs.add(job);
            }
        }

        currentUser.setSavedJobs(currentSavedJobs);
        this.userRepository.save(currentUser);

        return this.convertToUserResponse(currentUser);
    }

    @Override
    public UserResponse handleUnsaveJobsForCurrentUser(List<Long> jobIds) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if (currentEmail.isEmpty()) {
            return null;
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null) {
            return null;
        }

        List<Job> currentSavedJobs = currentUser.getSavedJobs() != null
                ? new ArrayList<>(currentUser.getSavedJobs())
                : new ArrayList<>();

        currentSavedJobs.removeIf(job -> jobIds.contains(job.getJobId()));

        currentUser.setSavedJobs(currentSavedJobs);
        this.userRepository.save(currentUser);

        return this.convertToUserResponse(currentUser);
    }

    // functions for notifications feature
    @Override
    public ResultPaginationResponse handleGetCurrentUserNotifications(Pageable pageable) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if (currentEmail.isEmpty()) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        Page<Notification> page = this.notificationRepository
                .findByRecipient_UserId(currentUser.getUserId(), pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        return new ResultPaginationResponse(meta, page.getContent());
    }

    @Override
    public List<Notification> handleGetLatestNotifications() {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if (currentEmail.isEmpty()) {
            return new ArrayList<>();
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null) {
            return new ArrayList<>();
        }

        PageRequest pageRequest = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return this.notificationRepository
                .findByRecipient_UserId(currentUser.getUserId(), pageRequest)
                .getContent();
    }

    @Override
    public void handleMarkNotificationsAsSeen(List<Long> notificationIds) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if (currentEmail.isEmpty()) {
            return;
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null) {
            return;
        }

        List<Notification> notifications = this.notificationRepository
                .findByNotificationIdInAndRecipient_UserId(notificationIds, currentUser.getUserId());

        notifications.forEach(notification -> notification.setSeen(true));
        this.notificationRepository.saveAll(notifications);
    }

    // functions for follow recruiters feature
    @Override
    public List<Recruiter> handleGetCurrentUserFollowedRecruiters() {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if (currentEmail.isEmpty()) {
            return new ArrayList<>();
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null || currentUser.getFollowedRecruiters() == null) {
            return new ArrayList<>();
        }

        return currentUser.getFollowedRecruiters();
    }

    @Override
    public List<Map<String, Object>> handleCheckRecruitersFollowed(List<Long> recruiterIds) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if (currentEmail.isEmpty()) {
            return new ArrayList<>();
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        List<Long> followedIds = currentUser.getFollowedRecruiters() != null
                ? currentUser.getFollowedRecruiters().stream().map(Recruiter::getUserId).toList()
                : new ArrayList<>();

        return recruiterIds.stream()
                .map(recruiterId -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("recruiterId", recruiterId);
                    result.put("result", followedIds.contains(recruiterId));
                    return result;
                })
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse handleFollowRecruiters(List<Long> recruiterIds) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if (currentEmail.isEmpty()) {
            return null;
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null) {
            return null;
        }

        List<Recruiter> currentFollowed = currentUser.getFollowedRecruiters() != null
                ? new ArrayList<>(currentUser.getFollowedRecruiters())
                : new ArrayList<>();

        List<Recruiter> recruitersToAdd = this.recruiterRepository.findByUserIdIn(recruiterIds);

        for (Recruiter r : recruitersToAdd) {
            if (currentFollowed.stream().noneMatch(fr -> fr.getUserId() == r.getUserId())) {
                currentFollowed.add(r);
                this.notificationService.handleNotifyFollowRecruiter(r);
            }
        }

        currentUser.setFollowedRecruiters(currentFollowed);
        this.userRepository.save(currentUser);

        return this.convertToUserResponse(currentUser);
    }

    @Override
    public UserResponse handleUnfollowRecruiters(List<Long> recruiterIds) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if (currentEmail.isEmpty()) {
            return null;
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null) {
            return null;
        }

        List<Recruiter> currentFollowed = currentUser.getFollowedRecruiters() != null
                ? new ArrayList<>(currentUser.getFollowedRecruiters())
                : new ArrayList<>();

        List<Recruiter> recruitersToUnfollow = this.recruiterRepository.findByUserIdIn(recruiterIds);

        for (Recruiter r : recruitersToUnfollow) {
            if (currentFollowed.removeIf(fr -> fr.getUserId() == r.getUserId())) {
                this.notificationService.handleNotifyUnfollowRecruiter(r);
            }
        }

        currentUser.setFollowedRecruiters(currentFollowed);
        this.userRepository.save(currentUser);

        return this.convertToUserResponse(currentUser);
    }

    @Override
    public List<UserEnabledResponse> handleActivateUsers(List<Long> userIds) {
        return this.setUsersEnabled(userIds, true);
    }

    @Override
    public List<UserEnabledResponse> handleDeactivateUsers(List<Long> userIds) {
        return this.setUsersEnabled(userIds, false);
    }

    private List<UserEnabledResponse> setUsersEnabled(List<Long> userIds, boolean enabled) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<User> users = this.userRepository.findAllById(userIds);
        if (users.isEmpty()) {
            return new ArrayList<>();
        }

        users.forEach(u -> {
            u.setEnabled(enabled);
            this.emailNotificationService.handleSendUserEnabledEmail(
                    u.getContact().getEmail(), u.getUsername(), enabled
            );
        });
        this.userRepository.saveAll(users);

        return users.stream()
                .map(u -> new UserEnabledResponse(u.getUserId(), u.isEnabled()))
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse convertToUserResponse(User user) {
        UserResponse userResponse = new UserResponse();

        userResponse.setUserId(user.getUserId());
        userResponse.setContact(user.getContact());
        userResponse.setAddress(user.getAddress());
        userResponse.setUsername(user.getUsername());
        userResponse.setFullName(user.getFullName());
        userResponse.setAvatar(user.getAvatar());
        userResponse.setEnabled(user.isEnabled());
        userResponse.setCreatedAt(user.getCreatedAt());
        userResponse.setUpdatedAt(user.getUpdatedAt());

        if (user.getRole() != null) {
            UserResponse.RoleUser roleUser = new UserResponse.RoleUser();
            roleUser.setRoleId(user.getRole().getRoleId());
            roleUser.setName(user.getRole().getName());

            userResponse.setRole(roleUser);
        }

        return userResponse;
    }

}
