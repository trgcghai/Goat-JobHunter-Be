package iuh.fit.goat.service.impl;


import iuh.fit.goat.common.Role;
import iuh.fit.goat.dto.request.auth.LoginRequest;
import iuh.fit.goat.dto.request.auth.VerifyUserRequest;
import iuh.fit.goat.dto.response.applicant.ApplicantResponse;
import iuh.fit.goat.dto.response.auth.LoginResponse;
import iuh.fit.goat.dto.response.recruiter.RecruiterResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.entity.embeddable.Contact;
import iuh.fit.goat.exception.InvalidException;
//import iuh.fit.goat.repository.RecruiterRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.*;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final UserService userService;
    private final RedisService redisService;
    //    private final EmailNotificationService emailNotificationService;
//    private final ApplicantService applicantService;
//    private final RecruiterService recruiterService;
    private final CompanyService companyService;
    private final SecurityUtil securityUtil;
//    private final UserRepository userRepository;
//    private final RecruiterRepository recruiterRepository;
//    private final PasswordEncoder passwordEncoder;

    @Value("${minhdat.jwt.access-token-validity-in-seconds}")
    private long jwtAccessToken;
    @Value("${minhdat.jwt.refresh-token-validity-in-seconds}")
    private long jwtRefreshToken;
    @Value("${minhdat.verify-code-validity-in-seconds}")
    private long validityInSeconds;

    @Override
    public Object handleLogin(LoginRequest loginRequest, HttpServletResponse response) throws InvalidException {

        log.info("User: {}", this.userService.handleGetUserByEmail(loginRequest.getEmail()));
        log.info("loginRequest: {}", loginRequest);

        Authentication authentication = this.authenticationManagerBuilder.getObject()
                .authenticate(new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(), loginRequest.getPassword())
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Thử get User trước
        User user = this.userService.handleGetUserByEmail(loginRequest.getEmail());
        Company company = null;

        // Nếu không phải User thì thử get Company
        if (user == null) {
            company = this.companyService.handleGetCompanyByEmail(loginRequest.getEmail());
        }

        // Kiểm tra tồn tại
        if (user == null && company == null) {
            throw new InvalidException("Invalid account");
        }

        // Kiểm tra enabled
        Account account = user != null ? user : company;
        if (!account.isEnabled()) {
            throw new InvalidException("Account is locked");
        }

        log.info("Account logged in: {}", account);

        LoginResponse loginResponse = createLoginResponse(account);

        // Tạo token và lưu vào Redis
        String accessToken = this.securityUtil.createAccessToken(account.getEmail(), loginResponse);
        String refreshToken = this.securityUtil.createRefreshToken(account.getEmail(), loginResponse);

        System.out.println("Refresh Token Created in Login: " + refreshToken);

        // Lưu refresh token vào Redis
        this.redisService.saveWithTTL(
                "refresh:" + refreshToken,
                account.getEmail(),
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
        System.out.println("Refresh Token in Redis: " + refreshToken);
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
                currentUser.getEmail(),
                currentUser.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        LoginResponse loginResponse = createLoginResponse(currentUser);

        String newAccessToken = this.securityUtil.createAccessToken(email, loginResponse);
        String newRefreshToken = this.securityUtil.createRefreshToken(email, loginResponse);

        this.redisService.replaceKey(
                "refresh:" + refreshToken,
                "refresh:" + newRefreshToken,
                currentUser.getEmail(),
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
                .httpOnly(true)
                .secure(false) // for dev
                .sameSite("Lax") // for dev
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie deleteRefreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false) // for dev
                .sameSite("Lax") // for dev
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteAccessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefreshCookie.toString());
    }

    @Override
    public LoginResponse handleGetCurrentAccount() {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        User currentUser = this.userService.handleGetUserByEmail(currentEmail);

        LoginResponse response = new LoginResponse();

        if(currentUser != null) {
            response = createLoginResponse(currentUser);
        }

        return response;
    }
//
//    @Override
//    public ApplicantResponse handleRegisterApplicant(Applicant applicant) throws InvalidException {
//        if(this.userService.handleExistsByEmail(applicant.getContact().getEmail())) {
//            throw new InvalidException("Email exists: " + applicant.getContact().getEmail());
//        }
//
//        String hashPassword = this.passwordEncoder.encode(applicant.getPassword());
//        applicant.setPassword(hashPassword);
//
//        Applicant newApplicant = this.applicantService.handleCreateApplicant(applicant);
//        ApplicantResponse applicantResponse = this.applicantService.convertToApplicantResponse(newApplicant);
//
//        String verificationCode = SecurityUtil.generateVerificationCode();
//        this.redisService.saveWithTTL(
//                newApplicant.getContact().getEmail(),
//                verificationCode,
//                validityInSeconds,
//                TimeUnit.SECONDS
//        );
//        this.emailNotificationService.handleSendVerificationEmail(newApplicant.getContact().getEmail(), verificationCode);
//
//        return applicantResponse;
//    }
//
//    @Override
//    public RecruiterResponse handleRegisterRecruiter(Recruiter recruiter) throws InvalidException {
//        if(this.userService.handleExistsByEmail(recruiter.getContact().getEmail())) {
//            throw new InvalidException("Email exists: " + recruiter.getContact().getEmail());
//        }
//
//        String hashPassword = this.passwordEncoder.encode(recruiter.getPassword());
//        recruiter.setPassword(hashPassword);
//
//        Recruiter newRecruiter = this.recruiterService.handleCreateRecruiter(recruiter);
//
//        return this.recruiterService.convertToRecruiterResponse(newRecruiter);
//    }
//
//    @Override
//    public void handleVerifyUser(VerifyUserRequest verifyUser) throws InvalidException {
//        User user = this.userService.handleGetUserByEmail(verifyUser.getEmail());
//        if(user == null) {
//            throw new InvalidException("User not found");
//        }
//
//        String key = user.getContact().getEmail();
//        if(!this.redisService.hasKey(key)) {
//            throw new InvalidException("Verification code has expired");
//        }
//        if(!this.redisService.getValue(key).equalsIgnoreCase(verifyUser.getVerificationCode())) {
//            throw new InvalidException("Invalid verification code");
//        }
//
//        user.setEnabled(true);
//        this.userRepository.save(user);
//
//        this.redisService.deleteKey(key);
//    }
//
//    @Override
//    public void handleVerifyRecruiter(long id) throws InvalidException {
//        Recruiter recruiter = this.recruiterService.handleGetRecruiterById(id);
//        if (recruiter != null) {
//            recruiter.setEnabled(true);
//            this.recruiterRepository.save(recruiter);
//        } else {
//            throw new InvalidException("Recruiter not found");
//        }
//    }
//
//    @Override
//    public void handleResendCode(String email) throws InvalidException {
//        User user = this.userService.handleGetUserByEmail(email);
//        if(user == null) {
//            throw new InvalidException("User not found");
//        }
//
//        String key = user.getContact().getEmail();
//        String verificationCode = SecurityUtil.generateVerificationCode();
//        this.redisService.replaceKey(
//                key,
//                key,
//                verificationCode,
//                validityInSeconds,
//                TimeUnit.SECONDS
//        );
//        this.emailNotificationService.handleSendVerificationEmail(key, verificationCode);
//    }
//

    private LoginResponse createLoginResponse(Account account) {
        LoginResponse loginResponse = new LoginResponse();

        // Thông tin chung của Account
        loginResponse.setAccountId(account.getAccountId());
        loginResponse.setEmail(account.getEmail());
        loginResponse.setUsername(Objects.requireNonNullElse(account.getUsername(), ""));
        loginResponse.setAvatar(Objects.requireNonNullElse(account.getAvatar(), ""));
        loginResponse.setEnabled(account.isEnabled());

        iuh.fit.goat.entity.Role role = account.getRole();

        if (role != null) {
            role.getName();
            loginResponse.setRole(role);
        }

        // Thông tin riêng của User
        if (account instanceof User) {
            User user = (User) account;

            loginResponse.setPhone(user.getPhone());
            loginResponse.setDob(user.getDob());
            loginResponse.setAddresses(user.getAddresses());
            loginResponse.setGender(user.getGender());
            loginResponse.setFullName(Objects.requireNonNullElse(user.getFullName(), ""));
            loginResponse.setType(user instanceof Applicant ? Role.APPLICANT.getValue() : Role.RECRUITER.getValue());
        }
        // Thông tin riêng của Company
        else if (account instanceof Company company) {
            loginResponse.setPhone(company.getPhone());
            loginResponse.setAddresses(company.getAddresses());
            loginResponse.setFullName(Objects.requireNonNullElse(company.getName(), ""));
            loginResponse.setType(Role.COMPANY.getValue());
        }

        return loginResponse;
    }
}
