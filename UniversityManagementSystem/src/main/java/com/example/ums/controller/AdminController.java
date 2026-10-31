package com.example.ums.controller;

import com.example.ums.dto.AccountRequest;
import com.example.ums.dto.AccountResponse;
import com.example.ums.entity.Role;
import com.example.ums.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/** CRUD for admins. Who may call it is decided in SecurityConfig. */
@RestController
@RequestMapping("/api/admins")
public class AdminController {

    private final AccountService accountService;

    public AdminController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** 200 OK with the list. */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> list() {
        return ResponseEntity.ok(accountService.list(Role.ADMIN));
    }

    /** 200 OK, or 404 if the id does not exist. */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.get(Role.ADMIN, id));
    }

    /** 201 Created with a Location header pointing at the new record. */
    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountRequest request) {
        AccountResponse created = accountService.create(Role.ADMIN, request);
        return ResponseEntity.created(URI.create("/api/admins/" + created.id())).body(created);
    }

    /** 200 OK with the updated record. Leave "password" out to keep the current one. */
    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> update(@PathVariable Long id, @Valid @RequestBody AccountRequest request) {
        return ResponseEntity.ok(accountService.update(Role.ADMIN, id, request));
    }

    /** 204 No Content. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.delete(Role.ADMIN, id);
        return ResponseEntity.noContent().build();
    }
}
