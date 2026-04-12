package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.request.account.AccountEnabledRequest;
import iuh.fit.goat.dto.request.account.AccountLockedRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.account.AccountEnabledResponse;
import iuh.fit.goat.dto.response.account.AccountLockedResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/accounts")
    public ResponseEntity<ResultPaginationResponse> getAllAccounts(
            @Filter Specification<Account> spec, Pageable pageable
    ) {
        ResultPaginationResponse result = this.adminService.handleGetAllAccounts(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PutMapping("/locked")
    public ResponseEntity<List<AccountLockedResponse>> lockedAccounts(
            @RequestBody AccountLockedRequest request
    ) throws InvalidException
    {
        List<Long> accountIds = request.getAccountIds();
        if (accountIds == null || accountIds.isEmpty()) {
            throw new InvalidException("Account IDs list cannot be empty");
        }
        List<AccountLockedResponse> res = this.adminService.handleLockedAccounts(accountIds);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PutMapping("/unlocked")
    public ResponseEntity<List<AccountLockedResponse>> unlockedAccounts(
            @RequestBody AccountLockedRequest request
    ) throws InvalidException
    {
        List<Long> accountIds = request.getAccountIds();
        if (accountIds == null || accountIds.isEmpty()) {
            throw new InvalidException("Account IDs list cannot be empty");
        }
        List<AccountLockedResponse> res = this.adminService.handleUnlockedAccounts(accountIds);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PutMapping("/activate")
    public ResponseEntity<List<AccountEnabledResponse>> activateAccounts(
            @RequestBody AccountEnabledRequest request
    ) throws InvalidException
    {
        List<Long> accountIds = request.getAccountIds();
        if (accountIds == null || accountIds.isEmpty()) {
            throw new InvalidException("Account IDs list cannot be empty");
        }
        List<AccountEnabledResponse> res = this.adminService.handleActivateAccounts(accountIds);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PutMapping("/deactivate")
    public ResponseEntity<List<AccountEnabledResponse>> deactivateAccounts(
            @RequestBody AccountEnabledRequest request
    ) throws InvalidException
    {
        List<Long> accountIds = request.getAccountIds();
        if (accountIds == null || accountIds.isEmpty()) {
            throw new InvalidException("Account IDs list cannot be empty");
        }
        List<AccountEnabledResponse> res = this.adminService.handleDeactivateAccounts(accountIds);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }
}
