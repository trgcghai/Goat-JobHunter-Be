package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.auth.*;
import iuh.fit.goat.dto.response.auth.LoginResponse;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.AuthService;
import iuh.fit.goat.util.annotation.ApiMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response, HttpServletRequest request
    ) throws InvalidException {
        return ResponseEntity.ok(this.authService.handleLogin(loginRequest, response, request));
    }

    @GetMapping("/refresh")
    @ApiMessage("Refresh account")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = "refreshToken", defaultValue = "missingValue") String refreshToken,
            HttpServletResponse response
    ) throws InvalidException {
        return ResponseEntity.ok(this.authService.handleRefreshToken(refreshToken, response));
    }

    @PostMapping("/logout")
    @ApiMessage("Logout account")
    public ResponseEntity<Void> logout(
            @CookieValue("accessToken") String accessToken,
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response
    ) {
        this.authService.handleLogout(accessToken, refreshToken, response);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/account/users")
    @ApiMessage("Get information account")
    public ResponseEntity<Object> getCurrentAccount() {
        try {
            Object result = this.authService.handleGetCurrentAccount();
            return ResponseEntity.status(HttpStatus.OK).body(result);
        } catch (InvalidException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/register/users")
    public ResponseEntity<Object> registerUsers(@Valid @RequestBody RegisterUserRequest request) throws InvalidException {
        Object result = this.authService.handleRegisterUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping(value = "/register/companies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> registerCompanies(@Valid @ModelAttribute RegisterCompanyRequest request) throws InvalidException {
        Object result = this.authService.handleRegisterCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/verify")
    public ResponseEntity<Object> verifyAccount(@RequestBody VerifyAccountRequest verifyAccount) {
        try {
            this.authService.handleVerifyAccount(verifyAccount);
            return ResponseEntity.ok(Map.of("message", "Account verified successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<Object> resendVerificationCode(@RequestParam String email) {
        try {
            this.authService.handleResendCode(email);
            return ResponseEntity.ok(Map.of("message", "Verification code sent"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteMyAccount(@Valid @RequestBody DeleteAccountRequest request) throws InvalidException {
        this.authService.handleDeleteMyAccount(request.getPassword());
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

}
