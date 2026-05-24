package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.account.UserVisibilityResponse;
import iuh.fit.goat.dto.response.auth.LoginResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.Visibility;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.service.*;
import iuh.fit.goat.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private BlogService blogService;
    @Mock private InterviewService interviewService;
    @Mock private RedisService redisService;
    @Mock private EmailNotificationService emailNotificationService;
    @Mock private JobService jobService;
    @Mock private CompanyService companyService;
    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;
    @Mock private JobRepository jobRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private BlogRepository blogRepository;
    @Mock private InterviewRepository interviewRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks
    private UserServiceImpl userService;

    private User account;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setRoleId(7L);
        role.setName("USER");

        account = new User();
        account.setAccountId(99L);
        account.setEmail("me@example.com");
        account.setUsername("me");
        account.setPassword("old-hash");
        account.setRole(role);
        account.setEnabled(true);
    }

    @Test
    void handleUpdateMyVisibility_shouldPersistVisibility() throws Exception {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserEmail).thenReturn("me@example.com");
            when(accountRepository.findByEmailAndDeletedAtIsNull("me@example.com")).thenReturn(Optional.of(account));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserVisibilityResponse response = userService.handleUpdateMyVisibility(Visibility.PRIVATE);

            assertThat(response.getAccountId()).isEqualTo(99L);
            assertThat(response.getVisibility()).isEqualTo(Visibility.PRIVATE);
            assertThat(account.getVisibility()).isEqualTo(Visibility.PRIVATE);
            verify(accountRepository).save(account);
        }
    }

    @Test
    void handleUpdatePassword_shouldReplaceTokensAndReturnPayload() throws Exception {
        Role role = new Role();
        role.setRoleId(7L);
        role.setName("USER");
        account.setRole(role);

        LoginResponse loginResponse = new LoginResponse();
        LoginResponse.RoleAccount roleAccount = new LoginResponse.RoleAccount();
        roleAccount.setRoleId(7L);
        roleAccount.setName("USER");
        loginResponse.setRole(roleAccount);

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserEmail).thenReturn("me@example.com");
            when(accountRepository.findByEmailAndDeletedAtIsNull("me@example.com")).thenReturn(Optional.of(account));
            when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(securityUtil.createAccessToken(eq("me@example.com"), any(LoginResponse.class))).thenReturn("new-access");
            when(securityUtil.createRefreshToken(eq("me@example.com"), any(LoginResponse.class))).thenReturn("new-refresh");

            Map<String, Object> result = userService.handleUpdatePassword("new-pass", "old-refresh");

            assertThat(result).containsKeys("loginResponse", "refreshToken", "accessToken");
            assertThat(result.get("accessToken")).isEqualTo("new-access");
            assertThat(result.get("refreshToken")).isEqualTo("new-refresh");
            assertThat(((LoginResponse) result.get("loginResponse")).getRole().getName()).isEqualTo("USER");
            assertThat(account.getPassword()).isEqualTo("new-hash");
            verify(redisService).replaceKey("refresh:old-refresh", "refresh:new-refresh", "me@example.com", 0L, TimeUnit.SECONDS);
        }
    }

    @Test
    void handleUpdatePassword_shouldRejectUnauthenticatedUser() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserEmail).thenReturn("");

            assertThatThrownBy(() -> userService.handleUpdatePassword("new-pass", "refresh"))
                    .isInstanceOf(iuh.fit.goat.exception.InvalidException.class)
                    .hasMessageContaining("User not authenticated");
        }
    }

    @Test
    void handleUpdatePassword_userNotFound_shouldThrow() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserEmail).thenReturn("me@example.com");
            when(accountRepository.findByEmailAndDeletedAtIsNull("me@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.handleUpdatePassword("new-pass", "refresh"))
                    .isInstanceOf(iuh.fit.goat.exception.InvalidException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Test
    void handleUpdatePassword_passwordEncoderThrows_shouldPropagate() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserEmail).thenReturn("me@example.com");
            when(accountRepository.findByEmailAndDeletedAtIsNull("me@example.com")).thenReturn(Optional.of(account));
            when(passwordEncoder.encode("new-pass")).thenThrow(new RuntimeException("encode-fail"));

            assertThatThrownBy(() -> userService.handleUpdatePassword("new-pass", "refresh"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("encode-fail");
        }
    }

    @Test
    void handleUpdatePassword_createAccessTokenThrows_shouldPropagate() throws Exception {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserEmail).thenReturn("me@example.com");
            when(accountRepository.findByEmailAndDeletedAtIsNull("me@example.com")).thenReturn(Optional.of(account));
            when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");
            when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
            when(securityUtil.createAccessToken(eq("me@example.com"), any(LoginResponse.class))).thenThrow(new RuntimeException("token-fail"));

            assertThatThrownBy(() -> userService.handleUpdatePassword("new-pass", "refresh"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("token-fail");
        }
    }

    @Test
    void handleUpdatePassword_redisReplaceThrows_shouldPropagate() throws Exception {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserEmail).thenReturn("me@example.com");
            when(accountRepository.findByEmailAndDeletedAtIsNull("me@example.com")).thenReturn(Optional.of(account));
            when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");
            when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
            when(securityUtil.createAccessToken(eq("me@example.com"), any(LoginResponse.class))).thenReturn("a");
            when(securityUtil.createRefreshToken(eq("me@example.com"), any(LoginResponse.class))).thenReturn("r");
            doThrow(new RuntimeException("redis-fail")).when(redisService).replaceKey(anyString(), anyString(), anyString(), anyLong(), any(TimeUnit.class));

            assertThatThrownBy(() -> userService.handleUpdatePassword("new-pass", "old-refresh"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("redis-fail");
        }
    }

    @Test
    void handleUpdatePassword_companyAccount_mapsCompanyFields() throws Exception {
        iuh.fit.goat.entity.Company company = new iuh.fit.goat.entity.Company();
        company.setAccountId(555L);
        company.setEmail("comp@example.com");
        company.setName("Acme Co");
        company.setPassword("old");

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserEmail).thenReturn("me@example.com");
            when(accountRepository.findByEmailAndDeletedAtIsNull("me@example.com")).thenReturn(Optional.of(company));
            when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");
            when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
            when(securityUtil.createAccessToken(eq("me@example.com"), any(LoginResponse.class))).thenReturn("a");
            when(securityUtil.createRefreshToken(eq("me@example.com"), any(LoginResponse.class))).thenReturn("r");

            Map<String, Object> res = userService.handleUpdatePassword("new-pass", "old-refresh");
            LoginResponse lr = (LoginResponse) res.get("loginResponse");
            assertThat(lr.getType()).isEqualTo(iuh.fit.goat.common.Role.COMPANY.getValue());
            assertThat(company.getPassword()).isEqualTo("new-hash");
        }
    }

    @Test
    void handleCheckCurrentPassword_true_and_false() {
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserEmail).thenReturn("me@example.com");
            when(accountRepository.findByEmailAndDeletedAtIsNull("me@example.com")).thenReturn(Optional.of(account));
            when(passwordEncoder.matches("old-hash", account.getPassword())).thenReturn(true);

            boolean ok = userService.handleCheckCurrentPassword("old-hash");
            assertThat(ok).isTrue();

            when(passwordEncoder.matches("bad", account.getPassword())).thenReturn(false);
            boolean nok = userService.handleCheckCurrentPassword("bad");
            assertThat(nok).isFalse();
        }
    }
}