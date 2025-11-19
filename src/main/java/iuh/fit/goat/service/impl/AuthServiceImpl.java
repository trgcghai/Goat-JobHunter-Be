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
import iuh.fit.goat.entity.embeddable.Contact;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.RecruiterRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.*;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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


import java.util.*;
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
    private final RecruiterRepository recruiterRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${minhdat.jwt.access-token-validity-in-seconds}")
    private long jwtAccessToken;
    @Value("${minhdat.jwt.refresh-token-validity-in-seconds}")
    private long jwtRefreshToken;
    @Value("${minhdat.verify-code-validity-in-seconds}")
    private long validityInSeconds;

    @Override
    public Object handleLogin(LoginRequest loginRequest, HttpServletResponse response) throws InvalidException {
        Authentication authentication = this.authenticationManagerBuilder.getObject()
                .authenticate(new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(), loginRequest.getPassword())
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        LoginResponse loginResponse = new LoginResponse();
        User currentUser = this.userService.handleGetUserByEmail(loginRequest.getEmail());
        if (currentUser == null) {
            throw new InvalidException("Invalid account");
        }
        if(!currentUser.isEnabled()) {
            throw new InvalidException("Account is locked");
        }
        loginResponse.setUser(createUserLogin(currentUser));

        String accessToken = this.securityUtil.createAccessToken(currentUser.getContact().getEmail(), loginResponse);
        String refreshToken = this.securityUtil.createRefreshToken(currentUser.getContact().getEmail(), loginResponse);

        this.redisService.saveWithTTL(
                "refresh:" + refreshToken,
                currentUser.getContact().getEmail(),
                jwtRefreshToken,
                TimeUnit.SECONDS
        );

        ResponseCookie accessCookie = ResponseCookie
                .from("accessToken", accessToken)
                .httpOnly(true)
                .secure(false) // for dev
                .sameSite("Lax") // for dev
                .path("/")
                .maxAge(jwtAccessToken)
                .build();

        ResponseCookie refreshCookie = ResponseCookie
                .from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false) // for dev
                .sameSite("Lax") // for dev
                .path("/")
                .maxAge(jwtAccessToken)
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
        if (!this.redisService.hasKey("refresh:" + refreshToken)) {
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
        loginResponse.setUser(createUserLogin(currentUser));

        String newAccessToken = this.securityUtil.createAccessToken(email, loginResponse);
        String newRefreshToken = this.securityUtil.createAccessToken(email, loginResponse);

        this.redisService.replaceKey(
                "refresh:" + refreshToken,
                "refresh:" + newRefreshToken,
                currentUser.getContact().getEmail(),
                jwtRefreshToken,
                TimeUnit.SECONDS
        );

        ResponseCookie newAccessCookie = ResponseCookie
                .from("accessToken", newAccessToken)
                .httpOnly(true)
                .secure(false) // for dev
                .sameSite("Lax") // for dev
                .path("/")
                .maxAge(jwtAccessToken)
                .build();

        ResponseCookie newRefreshCookie = ResponseCookie
                .from("refreshToken", newRefreshToken)
                .httpOnly(true)
                .secure(false) // for dev
                .sameSite("Lax") // for dev
                .path("/")
                .maxAge(jwtRefreshToken)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, newAccessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, newRefreshCookie.toString());

        return loginResponse;
    }

    @Override
    public void handleLogout(String accessToken, String refreshToken, HttpServletResponse response) {
        this.redisService.deleteKey("refresh:" + refreshToken);
        this.redisService.saveWithTTL(
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
        LoginResponse.UserGetAccount userGetAccount = new LoginResponse.UserGetAccount();
        if(currentUser != null) {
            userGetAccount.setUser(createUserLogin(currentUser));
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

        String verificationCode = SecurityUtil.generateVerificationCode();
        this.redisService.saveWithTTL(
                newApplicant.getContact().getEmail(),
                verificationCode,
                validityInSeconds,
                TimeUnit.SECONDS
        );
        this.emailService.handleSendVerificationEmail(newApplicant.getContact().getEmail(), verificationCode);

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

        return this.recruiterService.convertToRecruiterResponse(newRecruiter);
    }

    @Override
    public void handleVerifyUser(VerifyUserRequest verifyUser) throws InvalidException {
        User user = this.userService.handleGetUserByEmail(verifyUser.getEmail());
        if(user == null) {
            throw new InvalidException("User not found");
        }

        String key = user.getContact().getEmail();
        if(!this.redisService.hasKey(key)) {
            throw new InvalidException("Verification code has expired");
        }
        if(!this.redisService.getValue(key).equalsIgnoreCase(verifyUser.getVerificationCode())) {
            throw new InvalidException("Invalid verification code");
        }

        user.setEnabled(true);
        this.userRepository.save(user);

        this.redisService.deleteKey(key);
    }

    @Override
    public void handleVerifyRecruiter(long id) throws InvalidException {
        Recruiter recruiter = this.recruiterService.handleGetRecruiterById(id);
        if (recruiter != null) {
            recruiter.setEnabled(true);
            this.recruiterRepository.save(recruiter);
        } else {
            throw new InvalidException("Recruiter not found");
        }
    }

    @Override
    public void handleResendCode(String email) throws InvalidException {
        User user = this.userService.handleGetUserByEmail(email);
        if(user == null) {
            throw new InvalidException("User not found");
        }

        String key = user.getContact().getEmail();
        String verificationCode = SecurityUtil.generateVerificationCode();
        this.redisService.replaceKey(
                key,
                key,
                verificationCode,
                validityInSeconds,
                TimeUnit.SECONDS
        );
        this.emailService.handleSendVerificationEmail(key, verificationCode);
    }

    private LoginResponse.UserLogin createUserLogin(User user) {

        LoginResponse.UserLogin currentUserLogin = new LoginResponse.UserLogin();

        currentUserLogin.setUserId(user.getUserId());
        currentUserLogin.setDob(user.getDob());
        currentUserLogin.setGender(user.getGender());
        currentUserLogin.setFullName(Objects.requireNonNullElse(user.getFullName(), ""));
        currentUserLogin.setUsername(Objects.requireNonNullElse(user.getUsername(), ""));
        currentUserLogin.setContact((Contact) Objects.requireNonNullElse(user.getContact(), ""));
        currentUserLogin.setAvatar(Objects.requireNonNullElse(user.getAvatar(), ""));
        currentUserLogin.setType(user instanceof Applicant ? Role.APPLICANT.getValue() : Role.RECRUITER.getValue());
        currentUserLogin.setRole((iuh.fit.goat.entity.Role) Objects.requireNonNullElse(user.getRole(), ""));
        currentUserLogin.setEnabled(user.isEnabled());


        return currentUserLogin;
    }
}
