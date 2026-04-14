package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.Role;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.account.AccountEnabledResponse;
import iuh.fit.goat.dto.response.account.AccountLockedResponse;
import iuh.fit.goat.dto.response.account.AccountResponse;
import iuh.fit.goat.dto.response.notification.DeviceNotificationResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.service.AdminService;
import iuh.fit.goat.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final EmailNotificationServiceImpl emailNotificationService;
    private final NotificationService notificationService;

    private final AccountRepository accountRepository;

    @Override
    public ResultPaginationResponse handleGetAllAccounts(Specification<Account> spec, Pageable pageable) {
        Specification<Account> otherSpec = (root, query, cb) -> cb.isNull(root.get("deletedAt"));
        Specification<Account> finalSpec = spec == null ? otherSpec : spec.and(otherSpec);
        Page<Account> page = this.accountRepository.findAll(finalSpec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<AccountResponse> responses = page.getContent().stream()
                .filter(Objects::nonNull)
                .filter(account -> !account.getRole().getName().equalsIgnoreCase(Role.ADMIN.getValue()))
                .map(this::handleConvertToAccountResponse)
                .toList();

        return new ResultPaginationResponse(meta, responses);
    }

    @Override
    public List<AccountLockedResponse> handleLockedAccounts(List<Long> accountIds) {
        return this.setAccountsLocked(accountIds, true);
    }

    @Override
    public List<AccountLockedResponse> handleUnlockedAccounts(List<Long> accountIds) {
        return this.setAccountsLocked(accountIds, false);
    }

    @Override
    public List<AccountEnabledResponse> handleActivateAccounts(List<Long> accountId) {
        return this.setAccountsEnabled(accountId, true);
    }

    @Override
    public List<AccountEnabledResponse> handleDeactivateAccounts(List<Long> accountId) {
        return this.setAccountsEnabled(accountId, false);
    }

    @Override
    public AccountResponse handleConvertToAccountResponse(Account account) {
        AccountResponse response = new AccountResponse();

        String fullName = account instanceof User ? ((User) account).getFullName() : ((Company) account).getName();
        String avatar = account instanceof User ? account.getAvatar() : ((Company) account).getLogo();
        String phone = account instanceof User ? ((User) account).getPhone() : ((Company) account).getPhone();

        response.setAccountId(account.getAccountId());
        response.setUsername(account.getUsername());
        response.setFullName(fullName);
        response.setEmail(account.getEmail());
        response.setAvatar(avatar);
        response.setEnabled(account.isEnabled());
        response.setLocked(account.isLocked());
        response.setVisibility(account.getVisibility());
        response.setPhone(phone);
        response.setRole(new AccountResponse.RoleAccount(account.getRole().getRoleId(), account.getRole().getName()));
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());

        return response;
    }

    private List<AccountLockedResponse> setAccountsLocked(List<Long> accountIds, boolean locked) {
        if (accountIds == null || accountIds.isEmpty()) return new ArrayList<>();

        List<Account> accounts = this.accountRepository.findAllByAccountIdInAndDeletedAtIsNull(accountIds);
        if (accounts.isEmpty()) return new ArrayList<>();

        accounts.forEach(a -> {
            a.setLocked(locked);
            this.emailNotificationService.handleSendAccountLockedEmail(
                    a.getEmail(), a.getUsername(), locked
            );
            if(locked) {
                this.notificationService.handleForceLogout(
                        a.getEmail(),
                        new DeviceNotificationResponse(
                                "Tài khoản bị khóa. Vui lòng liên hệ hỗ trợ để biết thêm chi tiết.",
                                "",
                                Instant.now()
                        )
                );
            }
        });
        this.accountRepository.saveAll(accounts);

        return accounts.stream()
                .map(a -> new AccountLockedResponse(a.getAccountId(), a.isLocked()))
                .toList();
    }

    private List<AccountEnabledResponse> setAccountsEnabled(List<Long> accountIds, boolean enabled) {
        if (accountIds == null || accountIds.isEmpty()) return new ArrayList<>();

        List<Account> accounts = this.accountRepository.findAllByAccountIdInAndDeletedAtIsNull(accountIds);
        if (accounts.isEmpty()) return new ArrayList<>();

        accounts.forEach(a -> {
            a.setEnabled(enabled);
            this.emailNotificationService.handleSendAccountEnabledEmail(
                    a.getEmail(), a.getUsername(), enabled
            );
        });
        this.accountRepository.saveAll(accounts);

        return accounts.stream()
                .map(u -> new AccountEnabledResponse(u.getAccountId(), u.isEnabled()))
                .toList();
    }
}
