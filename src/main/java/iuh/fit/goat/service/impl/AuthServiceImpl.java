package iuh.fit.goat.service.impl;


import iuh.fit.goat.common.Role;
import iuh.fit.goat.dto.request.auth.LoginRequest;
import iuh.fit.goat.dto.request.auth.RegisterCompanyRequest;
import iuh.fit.goat.dto.request.auth.RegisterUserRequest;
import iuh.fit.goat.dto.request.auth.VerifyAccountRequest;
import iuh.fit.goat.dto.response.auth.LoginResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.AccountRepository;
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
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;

    private final AccountService accountService;
    private final UserService userService;
    private final RedisService redisService;
    private final EmailNotificationService emailNotificationService;
    private final ApplicantService applicantService;
    private final RecruiterService recruiterService;
    private final CompanyService companyService;
    private final RoleService roleService;

    private final AccountRepository accountRepository;

    @Value("${minhdat.jwt.access-token-validity-in-seconds}")
    private long jwtAccessToken;
    @Value("${minhdat.jwt.refresh-token-validity-in-seconds}")
    private long jwtRefreshToken;
    @Value("${minhdat.verify-code-validity-in-seconds}")
    private long validityInSeconds;

    @Override
    public LoginResponse handleLogin(LoginRequest loginRequest, HttpServletResponse response) throws InvalidException {

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

        log.info("loginResponse logged in: {}", loginResponse);

        // Tạo token và lưu vào Redis
        String accessToken = this.securityUtil.createAccessToken(account.getEmail(), loginResponse);
        String refreshToken = this.securityUtil.createRefreshToken(account.getEmail(), loginResponse);

        log.info("Refresh Token Created in Login: {}", refreshToken);

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
    public LoginResponse handleRefreshToken(String refreshToken, HttpServletResponse response) throws InvalidException {
        if(refreshToken.equalsIgnoreCase("missingValue")) {
            throw new InvalidException("You don't have a refresh token at cookie");
        }
        log.info("Refresh Token in Redis: " + refreshToken);
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
    public Object handleGetCurrentAccount() throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not logged in"));

        User user = this.userService.handleGetUserByEmail(email);

        if (user == null) {
            throw new InvalidException("User not found");
        }

        // Kiểm tra loại user và trả về response tương ứng
        if (user instanceof Applicant applicant) {
            return this.applicantService.convertToApplicantResponse(applicant);
        } else if (user instanceof Recruiter recruiter) {
            return this.recruiterService.convertToRecruiterResponse(recruiter);
        }

        // Trường hợp là User thông thường
        return this.userService.convertToUserResponse(user);
    }

    @Override
    public Object handleRegisterUser(RegisterUserRequest request) throws InvalidException {

        // validate email existence
        if (this.userService.handleExistsByEmail(request.getEmail())) {
            throw new InvalidException("Email exists: " + request.getEmail());
        }

        // hash password
        String hashPassword = this.passwordEncoder.encode(request.getPassword());

        String type = request.getType().trim().toLowerCase();

        if ("applicant".equals(type)) {
            Applicant applicant = new Applicant();
            applicant.setUsername(request.getUsername());
            applicant.setFullName(request.getFullName());
            applicant.setEmail(request.getEmail());
            applicant.setPassword(hashPassword);
            applicant.setPhone(request.getPhone());
            applicant.setRole(this.roleService.handleGetRoleByName(Role.APPLICANT.getValue()));

            // create applicant to save to database
            Applicant newApplicant = this.applicantService.handleCreateApplicant(applicant);

            // create verification code
            String verificationCode = SecurityUtil.generateVerificationCode();

            // save verification code to redis
            this.redisService.saveWithTTL(
                    newApplicant.getEmail(),
                    verificationCode,
                    validityInSeconds,
                    TimeUnit.SECONDS
            );

            // send email
            this.emailNotificationService.handleSendVerificationEmail(newApplicant.getEmail(), verificationCode);

            // return response
            return this.applicantService.convertToApplicantResponse(newApplicant);

        } else if ("recruiter".equals(type)) {
            Company company = this.companyService.handleGetCompanyByName(request.getCompanyName());
            if(company == null) throw new InvalidException("Company not found");

            Recruiter recruiter = new Recruiter();
            recruiter.setUsername(request.getUsername());
            recruiter.setFullName(request.getFullName());
            recruiter.setEmail(request.getEmail());
            recruiter.setPassword(hashPassword);
            recruiter.setPhone(request.getPhone());
            recruiter.setCompany(company);
            recruiter.setRole(this.roleService.handleGetRoleByName(Role.RECRUITER.getValue()));

            // create recruiter to save to database
            Recruiter newRecruiter = this.recruiterService.handleCreateRecruiter(recruiter);

            // create verification code
            String verificationCode = SecurityUtil.generateVerificationCode();

            // save verification code to redis
            this.redisService.saveWithTTL(
                    newRecruiter.getEmail(),
                    verificationCode,
                    validityInSeconds,
                    TimeUnit.SECONDS
            );

            // send email
            this.emailNotificationService.handleSendVerificationEmail(newRecruiter.getEmail(), verificationCode);

            // return response
            return this.recruiterService.convertToRecruiterResponse(newRecruiter);

        } else {
            throw new InvalidException("Unsupported type: " + request.getType());
        }
    }

    @Override
    public Object handleRegisterCompany(RegisterCompanyRequest request) throws InvalidException {
        if (this.accountService.handleGetAccountByEmail(request.getEmail()) != null) {
            throw new InvalidException("Email exists: " + request.getEmail());
        }

        if(this.companyService.handleGetCompanyByName(request.getName()) != null) {
            throw new InvalidException("Company name exists: " + request.getName());
        }

        String hashPassword = this.passwordEncoder.encode(request.getPassword());

        Company company = new Company();
        company.setUsername(request.getUsername());
        company.setEmail(request.getEmail());
        company.setPassword(hashPassword);
        company.setName(request.getName());
        company.setDescription(request.getDescription());
        company.setLogo(request.getLogo());
        company.setCoverPhoto(request.getCoverPhoto());
        company.setPhone(request.getPhone());
        company.setSize(request.getSize());
        company.setCountry(request.getCountry());
        company.setIndustry(request.getIndustry());
        company.setWorkingDays(request.getWorkingDays());
        company.setOvertimePolicy(request.getOvertimePolicy());
        if(request.getWebsite() != null) company.setWebsite(request.getWebsite());
        request.getAddresses().forEach(company::addAddress);

        Company newCompany = this.companyService.handleCreateCompany(company);

        String verificationCode = SecurityUtil.generateVerificationCode();
        this.redisService.saveWithTTL(
                newCompany.getEmail(),
                verificationCode,
                validityInSeconds,
                TimeUnit.SECONDS
        );
        this.emailNotificationService.handleSendVerificationEmail(newCompany.getEmail(), verificationCode);

        return this.companyService.convertToCompanyResponse(newCompany);
    }

    @Override
    public void handleVerifyAccount(VerifyAccountRequest verifyAccount) throws InvalidException {
        Account account = this.accountService.handleGetAccountByEmail(verifyAccount.getEmail());
        if(account == null) {
            throw new InvalidException("Account not found");
        }

        String key = account.getEmail();
        if(!this.redisService.hasKey(key)) {
            throw new InvalidException("Verification code has expired");
        }
        if(!this.redisService.getValue(key).equalsIgnoreCase(verifyAccount.getVerificationCode())) {
            throw new InvalidException("Invalid verification code");
        }

        account.setEnabled(true);
        this.accountRepository.save(account);

        this.redisService.deleteKey(key);
    }

    @Override
    public void handleResendCode(String email) throws InvalidException {
        Account account = this.accountService.handleGetAccountByEmail(email);
        if(account == null) {
            throw new InvalidException("Account not found");
        }

        String key = account.getEmail();
        String verificationCode = SecurityUtil.generateVerificationCode();
        this.redisService.replaceKey(
                key,
                key,
                verificationCode,
                validityInSeconds,
                TimeUnit.SECONDS
        );
        this.emailNotificationService.handleSendVerificationEmail(key, verificationCode);
    }

    private LoginResponse createLoginResponse(Account account) {
        LoginResponse loginResponse = new LoginResponse();

        // Thông tin chung của Account
        loginResponse.setAccountId(account.getAccountId());
        loginResponse.setEmail(account.getEmail());
        loginResponse.setUsername(Objects.requireNonNullElse(account.getUsername(), ""));
        loginResponse.setAvatar(Objects.requireNonNullElse(account.getAvatar(), ""));
        loginResponse.setEnabled(account.isEnabled());
        loginResponse.setAddresses(Objects.requireNonNullElse(account.getAddresses(), new ArrayList<>()));

        if (account.getRole() != null) {
            loginResponse.setRole(account.getRole());
        }

        // Thông tin riêng của User
        if (account instanceof User user) {
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
