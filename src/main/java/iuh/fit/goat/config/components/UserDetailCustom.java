package iuh.fit.goat.config.components;

import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.service.AccountService;
import iuh.fit.goat.service.CompanyService;
import iuh.fit.goat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component("userDetailsService")
@RequiredArgsConstructor
public class UserDetailCustom implements UserDetailsService {
    private final UserService userService;
    private final CompanyService companyService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Thử tìm User trước
        User user = this.userService.handleGetUserByEmail(username);
        Company company = null;

        // Nếu không phải User thì thử tìm Company
        if (user == null) {
            company = this.companyService.handleGetCompanyByEmail(username);
        }

        // Kiểm tra tồn tại
        if (user == null && company == null) {
            throw new UsernameNotFoundException("Account not found: " + username);
        }

        Account account = user != null ? user : company;

        return new org.springframework.security.core.userdetails.User(
                account.getEmail(),
                account.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
