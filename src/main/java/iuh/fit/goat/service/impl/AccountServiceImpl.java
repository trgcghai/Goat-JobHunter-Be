package iuh.fit.goat.service.impl;

import iuh.fit.goat.entity.Account;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    public Account handleGetAccountByEmail(String email) {
        return accountRepository.findByEmail(email).orElse(null);
    }

}
