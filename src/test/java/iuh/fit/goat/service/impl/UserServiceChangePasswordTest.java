package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.auth.LoginResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.service.RedisService;
import iuh.fit.goat.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceChangePasswordTest {

    @Mock private AccountRepository accountRepository;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private iuh.fit.goat.util.SecurityUtil securityUtilInstance;
    @Mock private RedisService redisService;
    @Mock private iuh.fit.goat.service.JobService jobService;
    @Mock private iuh.fit.goat.service.CompanyService companyService;
    @Mock private iuh.fit.goat.service.BlogService blogService;
    @Mock private iuh.fit.goat.service.InterviewService interviewService;
    @Mock private iuh.fit.goat.service.EmailNotificationService emailNotificationService;
    @Mock private iuh.fit.goat.repository.UserRepository userRepository;
    @Mock private iuh.fit.goat.repository.NotificationRepository notificationRepository;
    @Mock private iuh.fit.goat.repository.BlogRepository blogRepository;
    @Mock private iuh.fit.goat.repository.InterviewRepository interviewRepository;

    @InjectMocks private UserServiceImpl userService;

    private final String currentEmail = "me@example.com";
    private User userAccount;
    private Company companyAccount;

    @BeforeEach
    void setUp() {
        userAccount = new User();
        userAccount.setAccountId(10L);
        userAccount.setEmail(currentEmail);
        userAccount.setUsername("u1");

        companyAccount = new Company();
        companyAccount.setAccountId(20L);
        companyAccount.setEmail("co@example.com");
        companyAccount.setName("Co");
    }

    @Test
    void handleUpdatePassword_success_userAccount_returnsTokensAndLoginResponse() throws Exception {
        String newPassword = "newPass";
        String oldRefresh = "oldRef";

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserEmail).thenReturn(currentEmail);

            when(accountRepository.findByEmailAndDeletedAtIsNull(currentEmail)).thenReturn(Optional.of(userAccount));
            when(passwordEncoder.encode(newPassword)).thenReturn("hashed");
            when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
            when(securityUtilInstance.createAccessToken(eq(currentEmail), any(LoginResponse.class))).thenReturn("accessTok");
            when(securityUtilInstance.createRefreshToken(eq(currentEmail), any(LoginResponse.class))).thenReturn("refreshTok");

            // set jwt refresh validity
            org.springframework.test.util.ReflectionTestUtils.setField(userService, "jwtRefreshToken", 3600L);

            Map<String, Object> res = userService.handleUpdatePassword(newPassword, oldRefresh);

            assertThat(res).containsKeys("loginResponse", "refreshToken", "accessToken");
            assertThat(res.get("accessToken")).isEqualTo("accessTok");
            assertThat(res.get("refreshToken")).isEqualTo("refreshTok");
            verify(redisService).replaceKey(eq("refresh:" + oldRefresh), eq("refresh:" + "refreshTok"), eq(currentEmail), anyLong(), eq(TimeUnit.SECONDS));
        }
    }

    @Test
    void handleUpdatePassword_success_companyAccount_returnsCompanyFields() throws Exception {
        String newPassword = "np";
        String oldRef = "r1";

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserEmail).thenReturn(companyAccount.getEmail());
            when(accountRepository.findByEmailAndDeletedAtIsNull(companyAccount.getEmail())).thenReturn(Optional.of(companyAccount));
            when(passwordEncoder.encode(newPassword)).thenReturn("ch");
            when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
            when(securityUtilInstance.createAccessToken(eq(companyAccount.getEmail()), any(LoginResponse.class))).thenReturn("a2");
            when(securityUtilInstance.createRefreshToken(eq(companyAccount.getEmail()), any(LoginResponse.class))).thenReturn("r2");

            Map<String, Object> out = userService.handleUpdatePassword(newPassword, oldRef);

            assertThat(out.get("accessToken")).isEqualTo("a2");
            assertThat(out.get("refreshToken")).isEqualTo("r2");
            LoginResponse lr = (LoginResponse) out.get("loginResponse");
            assertThat(lr.getName()).isEqualTo(companyAccount.getName());
        }
    }

    @Test
    void handleUpdatePassword_throwsWhenNotAuthenticated() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserEmail).thenReturn("");

            assertThatThrownBy(() -> userService.handleUpdatePassword("p", "r")).isInstanceOf(InvalidException.class);
        }
    }

    @Test
    void handleUpdatePassword_throwsWhenAccountNotFound() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserEmail).thenReturn("nope@example.com");
            when(accountRepository.findByEmailAndDeletedAtIsNull("nope@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.handleUpdatePassword("p", "r")).isInstanceOf(InvalidException.class);
        }
    }

    @Test
    void passwordIsHashedAndSaved() throws Exception {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserEmail).thenReturn(currentEmail);
            when(accountRepository.findByEmailAndDeletedAtIsNull(currentEmail)).thenReturn(Optional.of(userAccount));
            when(passwordEncoder.encode("np")).thenReturn("hpass");
            when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
            when(securityUtilInstance.createAccessToken(eq(currentEmail), any(LoginResponse.class))).thenReturn("a");
            when(securityUtilInstance.createRefreshToken(eq(currentEmail), any(LoginResponse.class))).thenReturn("r");

            Map<String, Object> out = userService.handleUpdatePassword("np", "old");

            // loginResponse isn't the Account; instead assert that repository.save was called with an account with hashed pwd
            verify(accountRepository).save(argThat(acc -> Objects.equals(acc.getPassword(), "hpass")));
        }
    }

    @Test
    void tokensAreCreatedUsingSecurityUtilInstance() throws Exception {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserEmail).thenReturn(currentEmail);
            when(accountRepository.findByEmailAndDeletedAtIsNull(currentEmail)).thenReturn(Optional.of(userAccount));
            when(passwordEncoder.encode("pw")).thenReturn("hpw");
            when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
            when(securityUtilInstance.createAccessToken(eq(currentEmail), any(LoginResponse.class))).thenReturn("tokA");
            when(securityUtilInstance.createRefreshToken(eq(currentEmail), any(LoginResponse.class))).thenReturn("tokR");

            Map<String, Object> out = userService.handleUpdatePassword("pw", "oldR");

            assertThat(out.get("accessToken")).isEqualTo("tokA");
            assertThat(out.get("refreshToken")).isEqualTo("tokR");
        }
    }

    @Test
    void redisReplaceKey_calledWithExpectedKeys() throws Exception {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserEmail).thenReturn(currentEmail);
            when(accountRepository.findByEmailAndDeletedAtIsNull(currentEmail)).thenReturn(Optional.of(userAccount));
            when(passwordEncoder.encode("x")).thenReturn("hx");
            when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
            when(securityUtilInstance.createAccessToken(eq(currentEmail), any(LoginResponse.class))).thenReturn("a1");
            when(securityUtilInstance.createRefreshToken(eq(currentEmail), any(LoginResponse.class))).thenReturn("r1");

            userService.handleUpdatePassword("x", "oldKey");

            verify(redisService).replaceKey(eq("refresh:oldKey"), eq("refresh:r1"), eq(currentEmail), anyLong(), eq(TimeUnit.SECONDS));
        }
    }

    @Test
    void loginResponse_containsUserSpecificFields() throws Exception {
        userAccount.setFullName("Full Name");
        userAccount.setPhone("0909");

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserEmail).thenReturn(currentEmail);
            when(accountRepository.findByEmailAndDeletedAtIsNull(currentEmail)).thenReturn(Optional.of(userAccount));
            when(passwordEncoder.encode(any())).thenReturn("h");
            when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
            when(securityUtilInstance.createAccessToken(eq(currentEmail), any(LoginResponse.class))).thenReturn("a");
            when(securityUtilInstance.createRefreshToken(eq(currentEmail), any(LoginResponse.class))).thenReturn("r");

            Map<String, Object> out = userService.handleUpdatePassword("p", "rr");
            LoginResponse lr = (LoginResponse) out.get("loginResponse");

            assertThat(lr.getFullName()).isEqualTo("Full Name");
            assertThat(lr.getPhone()).isEqualTo("0909");
        }
    }
}
