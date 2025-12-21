package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.auth.LoginRequest;
import iuh.fit.goat.dto.request.auth.RegisterUserRequest;
import iuh.fit.goat.dto.request.auth.VerifyUserRequest;
import iuh.fit.goat.dto.response.applicant.ApplicantResponse;
import iuh.fit.goat.dto.response.auth.LoginResponse;
import iuh.fit.goat.dto.response.recruiter.RecruiterResponse;
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

    @GetMapping("/auth/account/users")
    @ApiMessage("Get information account")
    public ResponseEntity<?> getCurrentAccount() {
        try {
            Object result = this.authService.handleGetCurrentAccount();
            return ResponseEntity.status(HttpStatus.OK).body(result);
        } catch (InvalidException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/register/users")
    public ResponseEntity<?> registerUsers(@Valid @RequestBody RegisterUserRequest request) throws InvalidException {
        Object result = this.authService.handleRegisterUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
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

//    @PatchMapping("/auth/verify/recruiter/{id}")
//    public ResponseEntity<?> verifyRecruiter(@PathVariable("id") long id) {
//        try {
//            this.authService.handleVerifyRecruiter(id);
//            return ResponseEntity.ok(Map.of("message", "Recruiter verified successfully"));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }
//
//    @PostMapping("/auth/resend")
//    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
//        try {
//            this.authService.handleResendCode(email);
//            return ResponseEntity.ok(Map.of("message", "Verification code sent"));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }

}
