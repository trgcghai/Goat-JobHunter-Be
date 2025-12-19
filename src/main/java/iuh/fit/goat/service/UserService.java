package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.user.CreateUserRequest;
import iuh.fit.goat.dto.request.user.ResetPasswordRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.user.UserEnabledResponse;
import iuh.fit.goat.dto.response.user.UserResponse;
import iuh.fit.goat.entity.Blog;
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

//    User handleGetUserById(long id);
//
//    User handleCreateUser(CreateUserRequest request) throws InvalidException;
//
//    ResultPaginationResponse handleGetAllUsers(Specification<User> spec, Pageable pageable);
//
//    boolean handleCheckCurrentPassword(String currentPassword);
//
//    Map<String, Object> handleUpdatePassword(String newPassword, String refreshToken);
//
//    void handleResetPassword(ResetPasswordRequest resetPasswordRequest) throws InvalidException;

    UserResponse convertToUserResponse(User user);

//    // Job related methods
//    ResultPaginationResponse handleGetCurrentUserSavedJobs(Pageable pageable);
//
//    List<Map<String, Object>> handleCheckJobsSaved(List<Long> jobIds);
//
//    UserResponse handleSaveJobsForCurrentUser(List<Long> jobIds);
//
//    UserResponse handleUnsaveJobsForCurrentUser(List<Long> jobIds);
//
//    // Blog related methods
//    ResultPaginationResponse handleGetCurrentUserLikedBlogs(Specification<Blog> spec, Pageable pageable);
//
//    List<Map<String, Object>> handleCheckBlogsLiked(List<Long> blogIds);
//
//    List<Map<String, Object>> handleLikeBlogs(List<Long> blogIds);
//
//    List<Map<String, Object>> handleUnlikeBlogs(List<Long> blogIds);
//
//    // Notification related methods
//    ResultPaginationResponse handleGetCurrentUserNotifications(Pageable pageable);
//
//    List<Notification> handleGetLatestNotifications();
//
//    void handleMarkNotificationsAsSeen(List<Long> notificationIds);
//
//    // Recruiter related methods
//    List<Recruiter> handleGetCurrentUserFollowedRecruiters();
//
//    List<Map<String, Object>> handleCheckRecruitersFollowed(List<Long> recruiterIds);
//
//    UserResponse handleFollowRecruiters(List<Long> recruiterIds);
//
//    UserResponse handleUnfollowRecruiters(List<Long> recruiterIds);
//
//    // Admin related methods
//    List<UserEnabledResponse> handleActivateUsers(List<Long> userIds);
//
//    List<UserEnabledResponse> handleDeactivateUsers(List<Long> userIds);

}
