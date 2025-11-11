package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.Role;
import iuh.fit.goat.dto.request.LoginRequest;
import iuh.fit.goat.dto.request.VerifyUserRequest;
import iuh.fit.goat.dto.response.ApplicantResponse;
import iuh.fit.goat.dto.response.LoginResponse;
import iuh.fit.goat.dto.response.RecruiterResponse;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.*;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final UserService userService;
    private final RedisService redisService;
    private final EmailService emailService;
    private final ApplicantService applicantService;
    private final RecruiterService recruiterService;
    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${minhdat.jwt.access-token-validity-in-seconds}")
    private long jwtAccessToken;
    @Value("${minhdat.jwt.refresh-token-validity-in-seconds}")
    private long jwtRefreshToken;

    @Override
    public Object handleLogin(LoginRequest loginRequest, HttpServletResponse response) throws InvalidException {
        Authentication authentication = authenticationManagerBuilder.getObject()
                .authenticate(new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(), loginRequest.getPassword())
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        LoginResponse loginResponse = new LoginResponse();
        User currentUser = this.userService.handleGetUserByEmail(loginRequest.getEmail());
        System.out.println(currentUser);
        if (currentUser == null) {
            throw new InvalidException("Invalid account");
        }
        if(!currentUser.isEnabled()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message", "Account is locked"
                    )
            );
        }

        LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin(
                currentUser.getUserId(), currentUser.getContact().getEmail(),
                currentUser.getFullName() != null ? currentUser.getFullName() : "",
                currentUser.getUsername() != null ? currentUser.getUsername() : "",
                currentUser.getAvatar() != null ? currentUser.getAvatar() : "",
                currentUser instanceof Applicant ? Role.APPLICANT.getValue() : Role.RECRUITER.getValue(),
                true,
                currentUser.getRole(),
                Optional.ofNullable(currentUser.getSavedJobs()).orElse(Collections.emptyList()),
                Optional.ofNullable(currentUser.getFollowedRecruiters()).orElse(Collections.emptyList()),
                Optional.ofNullable(currentUser.getActorNotifications()).orElse(Collections.emptyList())
        );
        loginResponse.setUser(userLogin);

        String accessToken = this.securityUtil.createAccessToken(currentUser.getContact().getEmail(), loginResponse);
        String refreshToken = this.securityUtil.createRefreshToken(currentUser.getContact().getEmail(), loginResponse);

        this.redisService.setTokenWithTTL(
                "refresh:" + refreshToken,
                currentUser.getContact().getEmail(),
                jwtRefreshToken,
                TimeUnit.SECONDS
        );

        ResponseCookie accessCookie = ResponseCookie
                .from("accessToken", accessToken)
                .httpOnly(true)
//                .secure(true)
//                .sameSite("Strict")
                .path("/")
                .maxAge(jwtAccessToken)
                .build();

        ResponseCookie refreshCookie = ResponseCookie
                .from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
//                .sameSite("Strict")
//                .path("/")
                .maxAge(jwtRefreshToken)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return loginResponse;
    }

    @Override
    public Object handleRefreshToken(String refreshToken, HttpServletResponse response) throws InvalidException {
        if(refreshToken.equalsIgnoreCase("missingValue")) {
            throw new InvalidException("You don't have a refresh token at cookie");
        }
        if (!this.redisService.hasToken("refresh:" + refreshToken)) {
            throw new InvalidException("Invalid or expired refresh token");
        }

        Jwt jwt = this.securityUtil.checkValidToken(refreshToken);
        String email = jwt.getSubject();

        User currentUser = this.userService.handleGetUserByEmail(email);
        if(currentUser == null){
            throw new InvalidException("User not found");
        }
        if(!currentUser.isEnabled()) {
            throw new InvalidException("Account is locked");
        }

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                currentUser.getContact().getEmail(),
                currentUser.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        LoginResponse loginResponse = new LoginResponse();
        LoginResponse.UserLogin currentUserLogin = new LoginResponse.UserLogin();
        currentUserLogin.setUserId(currentUser.getUserId());
        currentUserLogin.setFullName(currentUser.getFullName());
        currentUserLogin.setEmail(currentUser.getContact().getEmail());
        currentUserLogin.setRole(currentUser.getRole());
        currentUserLogin.setSavedJobs(currentUser.getSavedJobs());
        currentUserLogin.setFollowedRecruiters(currentUser.getFollowedRecruiters());
        currentUserLogin.setActorNotifications(currentUser.getActorNotifications());
        loginResponse.setUser(currentUserLogin);

        String newAccessToken = this.securityUtil.createAccessToken(email, loginResponse);
        String newRefreshToken = this.securityUtil.createAccessToken(email, loginResponse);

        this.redisService.replaceToken(
                refreshToken, newRefreshToken,
                currentUser.getContact().getEmail(),
                jwtRefreshToken,
                TimeUnit.SECONDS
        );

        ResponseCookie newAccessCookie = ResponseCookie
                .from("accessToken", newAccessToken)
                .httpOnly(true)
//                .secure(true)
//                .sameSite("Strict")
                .path("/")
                .maxAge(jwtAccessToken)
                .build();

        ResponseCookie newRefreshCookie = ResponseCookie
                .from("refreshToken", newRefreshToken)
                .httpOnly(true)
//                .secure(true)
//                .sameSite("Strict")
                .path("/")
                .maxAge(jwtRefreshToken)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, newAccessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, newRefreshCookie.toString());

        return loginResponse;
    }

    @Override
    public void handleLogout(String accessToken, String refreshToken, HttpServletResponse response) {
        this.redisService.deleteToken("refresh:" + refreshToken);
        this.redisService.setTokenWithTTL(
                "blacklist:" + accessToken,
                "revoked",
                this.securityUtil.getRemainingTime(accessToken),
                TimeUnit.SECONDS

        );

        ResponseCookie deleteAccessCookie = ResponseCookie.from("accessToken", "")
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie deleteRefreshCookie = ResponseCookie.from("refreshToken", "")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteAccessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefreshCookie.toString());
    }

    @Override
    public LoginResponse.UserGetAccount handleGetCurrentAccount() {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        User currentUser = this.userService.handleGetUserByEmail(currentEmail);
        LoginResponse.UserLogin currentUserLogin = new LoginResponse.UserLogin();
        LoginResponse.UserGetAccount userGetAccount = new LoginResponse.UserGetAccount();
        if(currentUser != null) {
            currentUserLogin.setUserId(currentUser.getUserId());
            currentUserLogin.setFullName(currentUser.getFullName());
            currentUserLogin.setUsername(currentUser.getUsername());
            currentUserLogin.setEmail(currentUser.getContact().getEmail());
            currentUserLogin.setAvatar(currentUser.getAvatar());
            currentUserLogin.setType(currentUser instanceof Applicant ? Role.APPLICANT.getValue() : Role.RECRUITER.getValue());
            currentUserLogin.setRole(currentUser.getRole());
            currentUserLogin.setSavedJobs(currentUser.getSavedJobs());
            currentUserLogin.setFollowedRecruiters(currentUser.getFollowedRecruiters());
            currentUserLogin.setActorNotifications(currentUser.getActorNotifications());

            userGetAccount.setUser(currentUserLogin);
        }

        return userGetAccount;
    }

    @Override
    public ApplicantResponse handleRegisterApplicant(Applicant applicant) throws InvalidException {
        if(this.userService.handleExistsByEmail(applicant.getContact().getEmail())) {
            throw new InvalidException("Email exists: " + applicant.getContact().getEmail());
        }

        String hashPassword = this.passwordEncoder.encode(applicant.getPassword());
        applicant.setPassword(hashPassword);

        Applicant newApplicant = this.applicantService.handleCreateApplicant(applicant);
        ApplicantResponse applicantResponse = this.applicantService.convertToApplicantResponse(newApplicant);

        return applicantResponse;
    }

    @Override
    public RecruiterResponse handleRegisterRecruiter(Recruiter recruiter) throws InvalidException {
        if(this.userService.handleExistsByEmail(recruiter.getContact().getEmail())) {
            throw new InvalidException("Email exists: " + recruiter.getContact().getEmail());
        }

        String hashPassword = this.passwordEncoder.encode(recruiter.getPassword());
        recruiter.setPassword(hashPassword);

        Recruiter newRecruiter = this.recruiterService.handleCreateRecruiter(recruiter);
        RecruiterResponse recruiterResponse = this.recruiterService.convertToRecruiterResponse(newRecruiter);

        return recruiterResponse;
    }

    @Override
    public void handleVerifyUser(VerifyUserRequest verifyUser) throws InvalidException {
        User user = this.userService.handleGetUserByEmail(verifyUser.getEmail());
        if (user != null) {
            if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
                throw new InvalidException("Verification code has expired");
            }
            if (user.getVerificationCode().equals(verifyUser.getVerificationCode())) {
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationCodeExpiresAt(null);
                this.userRepository.save(user);
            } else {
                throw new InvalidException("Invalid verification code");
            }
        } else {
            throw new InvalidException("User not found");
        }
    }

    @Override
    public void handleResendCode(String email) throws InvalidException {
        User user = this.userService.handleGetUserByEmail(email);
        if (user != null) {
            user.setVerificationCode(SecurityUtil.generateVerificationCode());
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
            this.emailService.handleSendVerificationEmail(user);
            this.userRepository.save(user);
        } else {
            throw new InvalidException("User not found");
        }
    }

}
