package com.example.ums.config;

import com.example.ums.dto.AccountRequest;
import com.example.ums.entity.Role;
import com.example.ums.repository.AccountRepository;
import com.example.ums.service.AccountService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** On the very first start (empty database) creates the first admin, so there is someone to log in as. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(AccountRepository accountRepository, AccountService accountService,
                      @Value("${app.bootstrap.email}") String adminEmail,
                      @Value("${app.bootstrap.password}") String adminPassword) {
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (accountRepository.count() > 0) {
            return;
        }
        if (adminPassword.length() < 8) {
            throw new IllegalStateException("Set ADMIN_PASSWORD to at least 8 characters for the first start");
        }
        accountService.create(Role.ADMIN, new AccountRequest("University Admin", adminEmail, adminPassword,
                "Administration", null, null, null, null));
    }
}
