package com.example.ums.controller;

import com.example.ums.dto.OnCreate;
import com.example.ums.dto.TeacherRequest;
import com.example.ums.dto.TeacherResponse;
import com.example.ums.service.TeacherService;
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

/** CRUD for teachers. Who may call it is decided in SecurityConfig. */
@RestController
@RequestMapping("/api/teachers")
@Tag(name = "Teachers")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    /** 200 OK with the list. */
    @GetMapping
    @Operation(summary = "List all teachers")
    public ResponseEntity<List<TeacherResponse>> getAll() {
        return ResponseEntity.ok(teacherService.findAll());
    }

    /** 200 OK, or 404 if the id does not exist. */
    @GetMapping("/{id}")
    @Operation(summary = "Get one teacher")
    public ResponseEntity<TeacherResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(teacherService.findById(id));
    }

    /** 201 Created with a Location header pointing at the new record. */
    @PostMapping
    @Operation(summary = "Create a teacher")
    public ResponseEntity<TeacherResponse> create(
            @Validated({OnCreate.class, Default.class}) @RequestBody TeacherRequest request) {
        TeacherResponse created = teacherService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** 200 OK with the updated record. Leave "password" out to keep the current one. */
    @PutMapping("/{id}")
    @Operation(summary = "Update a teacher")
    public ResponseEntity<TeacherResponse> update(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        return ResponseEntity.ok(teacherService.update(id, request));
    }

    /** 204 No Content. */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a teacher")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teacherService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
