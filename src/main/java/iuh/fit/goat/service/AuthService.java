package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.auth.LoginRequest;
import iuh.fit.goat.dto.request.auth.RegisterCompanyRequest;
import iuh.fit.goat.dto.request.auth.RegisterUserRequest;
import iuh.fit.goat.dto.request.auth.VerifyAccountRequest;
import iuh.fit.goat.dto.response.auth.LoginResponse;
import iuh.fit.goat.exception.InvalidException;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    LoginResponse handleLogin(LoginRequest loginRequest, HttpServletResponse response) throws InvalidException;

    LoginResponse handleRefreshToken(String refreshToken, HttpServletResponse response) throws InvalidException;

    void handleLogout(String accessToken, String refreshToken, HttpServletResponse response);

    Object handleGetCurrentAccount() throws InvalidException;

    Object handleRegisterUser(RegisterUserRequest request) throws InvalidException;

    Object handleRegisterCompany(RegisterCompanyRequest request) throws InvalidException;

    void handleVerifyAccount(VerifyAccountRequest verifyAccount) throws InvalidException;

    void handleResendCode(String email) throws InvalidException;
}
