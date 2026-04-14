package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.account.AccountLockedResponse;
import iuh.fit.goat.dto.response.account.AccountResponse;
import iuh.fit.goat.dto.response.account.AccountEnabledResponse;
import iuh.fit.goat.entity.Account;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface AdminService {
    ResultPaginationResponse handleGetAllAccounts(Specification<Account> spec, Pageable pageable);

    List<AccountLockedResponse> handleLockedAccounts(List<Long> accountIds);

    List<AccountLockedResponse> handleUnlockedAccounts(List<Long> accountIds);

    List<AccountEnabledResponse> handleActivateAccounts(List<Long> accountId);

    List<AccountEnabledResponse> handleDeactivateAccounts(List<Long> accountId);

    AccountResponse handleConvertToAccountResponse(Account account);
}
