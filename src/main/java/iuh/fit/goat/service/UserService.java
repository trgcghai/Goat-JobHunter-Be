package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.user.CreateUserRequest;
import iuh.fit.goat.dto.request.user.ResetPasswordRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.user.UserEnabledResponse;
import iuh.fit.goat.dto.response.user.UserResponse;
import iuh.fit.goat.entity.*;
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

    /*     ========================= Saved Job Related Methods =========================  */

    ResultPaginationResponse handleGetCurrentUserSavedJobs(Pageable pageable);

    List<Map<String, Object>> handleCheckJobsSaved(List<Long> jobIds);

    UserResponse handleSaveJobsForCurrentUser(List<Long> jobIds);

    UserResponse handleUnsaveJobsForCurrentUser(List<Long> jobIds);

    /*     ========================= ========================= =========================  */

    /*     ========================= Saved Blogs Related Methods =========================  */

    ResultPaginationResponse handleGetCurrentUserSavedBlogs(Specification<Blog> spec, Pageable pageable);

    List<Map<String, Object>> handleCheckBlogsSaved(List<Long> blogIds);

    UserResponse handleSaveBlogsForCurrentUser(List<Long> blogIds);

    UserResponse handleUnsaveBlogsForCurrentUser(List<Long> blogIds);

    /*     ========================= ========================= =========================  */

//    // Notification related methods
//    ResultPaginationResponse handleGetCurrentUserNotifications(Pageable pageable);
//
//    List<Notification> handleGetLatestNotifications();
//
//    void handleMarkNotificationsAsSeen(List<Long> notificationIds);
//
    /*     ========================= Followed Companies Related Endpoints =========================  */
    List<Company> handleGetCurrentUserFollowedCompanies();

    List<Map<String, Object>> handleCheckCompaniesFollowed(List<Long> companyIds);

    boolean handleFollowCompanies(List<Long> companyIds);

    boolean handleUnfollowCompanies(List<Long> companyIds);
    /*     ========================= ========================= =========================  */

    /*     ========================= Reviewed Companies Related Endpoints =========================  */
    List<Map<String, Object>> handleCheckReviewedCompanies(List<Long> companyIds);
    /*     ========================= ========================= =========================  */

//
//    // Admin related methods
//    List<UserEnabledResponse> handleActivateUsers(List<Long> userIds);
//
//    List<UserEnabledResponse> handleDeactivateUsers(List<Long> userIds);

}
