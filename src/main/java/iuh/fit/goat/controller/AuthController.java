package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.LoginRequest;
import iuh.fit.goat.dto.request.VerifyUserRequest;
import iuh.fit.goat.dto.response.ApplicantResponse;
import iuh.fit.goat.dto.response.LoginResponse;
import iuh.fit.goat.dto.response.RecruiterResponse;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.AuthService;
import iuh.fit.goat.util.annotation.ApiMessage;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response
    ) throws InvalidException {
        return ResponseEntity.ok(this.authService.handleLogin(loginRequest, response));
    }

    @GetMapping("/auth/refresh")
    @ApiMessage("Refresh account")
    public ResponseEntity<?> refresh(
            @CookieValue(name = "refreshToken", defaultValue = "missingValue") String refreshToken,
            HttpServletResponse response
    ) throws InvalidException {
        return ResponseEntity.ok(this.authService.handleRefreshToken(refreshToken, response));
    }

    @PostMapping("/auth/logout")
    @ApiMessage("Logout account")
    public ResponseEntity<Void> logout(
            @CookieValue("accessToken") String accessToken,
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response
    ) {
        this.authService.handleLogout(accessToken, refreshToken, response);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/auth/account")
    @ApiMessage("Get information account")
    public ResponseEntity<LoginResponse.UserGetAccount> getCurrentAccount(){
        LoginResponse.UserGetAccount userGetAccount = this.authService.handleGetCurrentAccount();
        return ResponseEntity.status(HttpStatus.OK).body(userGetAccount);
    }

    @PostMapping("/auth/register/applicant")
    public ResponseEntity<ApplicantResponse> registerApplicant(
            @Valid @RequestBody Applicant applicant
    ) throws InvalidException {
        ApplicantResponse applicantResponse = this.authService.handleRegisterApplicant(applicant);
        return ResponseEntity.status(HttpStatus.CREATED).body(applicantResponse);
    }

    @PostMapping("/auth/register/recruiter")
    public ResponseEntity<RecruiterResponse> registerRecruiter(
            @Valid @RequestBody Recruiter recruiter
    ) throws InvalidException {
        RecruiterResponse recruiterResponse = this.authService.handleRegisterRecruiter(recruiter);
        return ResponseEntity.status(HttpStatus.CREATED).body(recruiterResponse);
    }

    @PostMapping("/auth/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserRequest verifyUser) {
        try {
            this.authService.handleVerifyUser(verifyUser);
            return ResponseEntity.ok(Map.of("message", "Account verified successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        try {
            this.authService.handleResendCode(email);
            return ResponseEntity.ok(Map.of("message", "Verification code sent"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
