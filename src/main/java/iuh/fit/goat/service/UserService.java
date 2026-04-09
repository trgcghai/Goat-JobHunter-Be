package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.user.ResetPasswordRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.company.CompanyResponse;
import iuh.fit.goat.dto.response.user.UserEnabledResponse;
import iuh.fit.goat.dto.response.user.UserResponse;
import iuh.fit.goat.dto.response.user.UserVisibilityResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.Visibility;
import iuh.fit.goat.exception.InvalidException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

public interface UserService {
    Account handleGetAccountByEmail(String email);

    boolean handleExistsByEmail(String email);

    User handleGetUserById(long id);

    ResultPaginationResponse handleGetAllUsers(Specification<User> spec, Pageable pageable);

    ResultPaginationResponse handleSearchUsers(String searchTerm, Pageable pageable) throws InvalidException;

    boolean handleCheckCurrentPassword(String currentPassword);

    Map<String, Object> handleUpdatePassword(String newPassword, String refreshToken) throws InvalidException;

    void handleResetPassword(ResetPasswordRequest resetPasswordRequest) throws InvalidException;

    UserResponse convertToUserResponse(User user);

    /*     ========================= Saved Job Related Methods =========================  */

    ResultPaginationResponse handleGetCurrentAccountSavedJobs(Pageable pageable);

    List<Map<String, Object>> handleCheckJobsSaved(List<Long> jobIds);

    Object handleSaveJobsForCurrentAccount(List<Long> jobIds);

    Object handleUnsaveJobsForCurrentAccount(List<Long> jobIds);

    /*     ========================= ========================= =========================  */

    /*     ========================= Saved Blogs Related Methods =========================  */

    ResultPaginationResponse handleGetCurrentAccountSavedBlogs(Specification<Blog> spec, Pageable pageable);

    List<Map<String, Object>> handleCheckBlogsSaved(List<Long> blogIds);

    Object handleSaveBlogsForCurrentAccount(List<Long> blogIds);

    Object handleUnsaveBlogsForCurrentAccount(List<Long> blogIds);

    /*     ========================= ========================= =========================  */

    // Notification related methods
    ResultPaginationResponse handleGetCurrentUserNotifications(Pageable pageable);

    List<Notification> handleGetLatestNotifications();

    void handleMarkNotificationsAsSeen(List<Long> notificationIds);

    /*     ========================= Followed Companies Related Endpoints =========================  */
    List<CompanyResponse> handleGetCurrentAccountFollowedCompanies();

    List<Map<String, Object>> handleCheckCompaniesFollowed(List<Long> companyIds);

    boolean handleFollowCompanies(List<Long> companyIds);

    boolean handleUnfollowCompanies(List<Long> companyIds);
    /*     ========================= ========================= =========================  */

    /*     ========================= Reviewed Companies Related Endpoints =========================  */
    List<Map<String, Object>> handleCheckReviewedCompanies(List<Long> companyIds);
    /*     ========================= ========================= =========================  */

    /*     ========================= Interview Related Endpoints =========================  */
    ResultPaginationResponse handleGetCurrentUserInterviews(Specification<Interview> spec, Pageable pageable);
    /*     ========================= ========================= =========================  */

    // Admin related methods
    List<UserEnabledResponse> handleActivateUsers(List<Long> userIds);

    List<UserEnabledResponse> handleDeactivateUsers(List<Long> userIds);

    UserVisibilityResponse handleUpdateMyVisibility(Visibility visibility) throws InvalidException;

    List<UserVisibilityResponse> handleUpdateUsersVisibility(List<Long> accountIds, Visibility visibility) throws InvalidException;

}
