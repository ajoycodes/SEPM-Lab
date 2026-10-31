package com.example.ums.security;

import com.example.ums.entity.Account;
import com.example.ums.entity.Role;
import com.example.ums.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Answers "who is making this request?". */
@Component
public class CurrentUser {

    private final AccountRepository accountRepository;

    public CurrentUser(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /** The logged-in account, or 401 if there is none (for example, it was deleted during the session). */
    public Account require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            return accountRepository.findById(principal.accountId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again"));
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again");
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getPrincipal() instanceof AccountPrincipal principal
                && principal.role() == Role.ADMIN;
    }
}
