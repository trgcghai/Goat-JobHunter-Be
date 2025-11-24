package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.FollowRecruiterRequest;
import iuh.fit.goat.dto.request.ResetPasswordRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.UserResponse;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.entity.Notification;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

public interface UserService {
    User handleGetUserByEmail(String email);

    boolean handleExistsByEmail(String email);

    User handleGetUserById(long id);

    ResultPaginationResponse handleGetAllUsers(Specification<User> spec, Pageable pageable);

    boolean handleCheckCurrentPassword(String currentPassword);

    Map<String, Object> handleUpdatePassword(String newPassword, String refreshToken);

    void handleResetPassword(ResetPasswordRequest resetPasswordRequest) throws InvalidException;

    UserResponse convertToUserResponse(User user);

    List<Job> handleGetCurrentUserSavedJobs();

    List<Map<String, Object>> handleCheckJobsSaved(List<Long> jobIds);

    UserResponse handleSaveJobsForCurrentUser(List<Long> jobIds);

    UserResponse handleUnsaveJobsForCurrentUser(List<Long> jobIds);

    ResultPaginationResponse handleGetCurrentUserNotifications(Pageable pageable);

    List<Notification> handleGetLatestNotifications();

    void handleMarkNotificationsAsSeen(List<Long> notificationIds);

    List<Recruiter> handleGetCurrentUserFollowedRecruiters();

    List<Map<String, Object>> handleCheckRecruitersFollowed(List<Long> recruiterIds);

    UserResponse handleFollowRecruiters(List<Long> recruiterIds);

    UserResponse handleUnfollowRecruiters(List<Long> recruiterIds);
}
