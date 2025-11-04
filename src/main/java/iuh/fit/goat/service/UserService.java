package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.FollowRecruiterRequest;
import iuh.fit.goat.dto.request.ResetPasswordRequest;
import iuh.fit.goat.dto.request.SaveJobRequest;
import iuh.fit.goat.dto.request.VerifyUserRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.UserResponse;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

public interface UserService {
    User handleGetUserByEmail(String email);

    boolean handleExistsByEmail(String email);

    User handleGetUserById(long id);

    void handleUpdateRefreshToken(String email, String refreshToken);

    User handleGetUserByRefreshTokenAndEmail(String refreshToken, String email);

    ResultPaginationResponse handleGetAllUsers(Specification<User> spec, Pageable pageable);

    boolean handleCheckCurrentPassword(String currentPassword);

    Map<String, Object> handleUpdatePassword(String newPassword);

    void handleResetPassword(ResetPasswordRequest resetPasswordRequest) throws InvalidException;

    UserResponse handleSaveJobs(SaveJobRequest saveJobRequest);

    UserResponse handleFollowRecruiters(FollowRecruiterRequest followRecruiterRequest);

    void handleVerifyUser(VerifyUserRequest verifyUser) throws InvalidException;

    void handleResendCode(String email) throws InvalidException;

    UserResponse convertToUserResponse(User user);
}
