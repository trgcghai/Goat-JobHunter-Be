package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.auth.LoginRequest;
import iuh.fit.goat.dto.request.auth.RegisterUserRequest;
import iuh.fit.goat.dto.request.auth.VerifyUserRequest;
import iuh.fit.goat.dto.response.applicant.ApplicantResponse;
import iuh.fit.goat.dto.response.auth.LoginResponse;
import iuh.fit.goat.dto.response.recruiter.RecruiterResponse;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.exception.InvalidException;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    Object handleLogin(LoginRequest loginRequest, HttpServletResponse response) throws InvalidException;

    Object handleRefreshToken(String refreshToken, HttpServletResponse response) throws InvalidException;

    void handleLogout(String accessToken, String refreshToken, HttpServletResponse response);

    Object handleGetCurrentAccount() throws InvalidException;

    Object handleRegisterUser(RegisterUserRequest request) throws InvalidException;

    void handleVerifyUser(VerifyUserRequest verifyUser) throws InvalidException;
//
//    void handleVerifyRecruiter(long id) throws InvalidException;
//
//    void handleResendCode(String email) throws InvalidException;
}
