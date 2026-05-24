package iuh.fit.goat.service.impl;

import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User account;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setRoleId(1L);
        role.setName("USER");

        account = new User();
        account.setAccountId(10L);
        account.setEmail("user@example.com");
        account.setUsername("user1");
        account.setPassword("secret");
        account.setRole(role);
    }

    @Test
    void handleGetAccountByEmail_shouldReturnAccountWhenFound() {
        when(accountRepository.findByEmailWithRole("user@example.com")).thenReturn(Optional.of(account));

        Account result = accountService.handleGetAccountByEmail("user@example.com");

        assertThat(result).isSameAs(account);
    }

    @Test
    void handleGetAccountByEmail_shouldReturnNullWhenMissing() {
        when(accountRepository.findByEmailWithRole("missing@example.com")).thenReturn(Optional.empty());

        Account result = accountService.handleGetAccountByEmail("missing@example.com");

        assertThat(result).isNull();
    }
}