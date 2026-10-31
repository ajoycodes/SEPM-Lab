package com.example.ums.security;

import com.example.ums.entity.Account;
import com.example.ums.repository.AccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Spring Security calls this during login. It loads the account by email;
 * Spring then checks the typed password against the stored BCrypt hash.
 */
@Service
public class AccountUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    public AccountUserDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Account account = accountRepository.findByEmail(email.strip().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return new AccountPrincipal(account.getId(), account.getEmail(), account.getPasswordHash(), account.getRole());
    }
}
