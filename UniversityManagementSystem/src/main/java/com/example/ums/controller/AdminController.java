package com.example.ums.controller;

import com.example.ums.dto.OnCreate;
import com.example.ums.dto.AdminRequest;
import com.example.ums.dto.AdminResponse;
import com.example.ums.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** CRUD for admins. Who may call it is decided in SecurityConfig. */
@RestController
@RequestMapping("/api/admins")
@Tag(name = "Admins")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** 200 OK with the list. */
    @GetMapping
    @Operation(summary = "List all admins")
    public ResponseEntity<List<AdminResponse>> getAll() {
        return ResponseEntity.ok(adminService.findAll());
    }

    /** 200 OK, or 404 if the id does not exist. */
    @GetMapping("/{id}")
    @Operation(summary = "Get one admin")
    public ResponseEntity<AdminResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.findById(id));
    }

    /** 201 Created with a Location header pointing at the new record. */
    @PostMapping
    @Operation(summary = "Create an admin")
    public ResponseEntity<AdminResponse> create(
            @Validated({OnCreate.class, Default.class}) @RequestBody AdminRequest request) {
        AdminResponse created = adminService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** 200 OK with the updated record. Leave "password" out to keep the current one. */
    @PutMapping("/{id}")
    @Operation(summary = "Update an admin")
    public ResponseEntity<AdminResponse> update(@PathVariable Long id, @Valid @RequestBody AdminRequest request) {
        return ResponseEntity.ok(adminService.update(id, request));
    }

    /** 204 No Content. */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an admin")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
