package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.LoginRequest;
import iuh.fit.goat.dto.request.VerifyUserRequest;
import iuh.fit.goat.dto.response.ApplicantResponse;
import iuh.fit.goat.dto.response.LoginResponse;
import iuh.fit.goat.dto.response.RecruiterResponse;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.exception.InvalidException;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    Object handleLogin(LoginRequest loginRequest, HttpServletResponse response) throws InvalidException;

    Object handleRefreshToken(String refreshToken, HttpServletResponse response) throws InvalidException;

    void handleLogout(String accessToken, String refreshToken, HttpServletResponse response);

    LoginResponse.UserGetAccount handleGetCurrentAccount();

    ApplicantResponse handleRegisterApplicant(Applicant applicant) throws InvalidException;

    RecruiterResponse handleRegisterRecruiter(Recruiter recruiter) throws InvalidException;

    void handleVerifyUser(VerifyUserRequest verifyUser) throws InvalidException;

    void handleVerifyRecruiter(long id) throws InvalidException;

    void handleResendCode(String email) throws InvalidException;
}
