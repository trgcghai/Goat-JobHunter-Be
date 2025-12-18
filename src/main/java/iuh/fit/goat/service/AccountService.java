package iuh.fit.goat.service;

import iuh.fit.goat.entity.Account;

public interface AccountService {
    Account handleGetAccountByEmail(String email);
}
