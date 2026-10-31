package com.example.ums.controller;

import com.example.ums.dto.ProfileRequest;
import com.example.ums.dto.TeacherProfile;
import com.example.ums.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /** The logged-in user's own profile. Its shape depends on their role. */
    @GetMapping("/api/profile/me")
    public Object own() {
        return profileService.own();
    }

    @PutMapping("/api/profile/me")
    public Object updateOwn(@Valid @RequestBody ProfileRequest request) {
        return profileService.updateOwn(request);
    }

    @GetMapping("/api/public/teachers")
    public List<TeacherProfile> teachers() {
        return profileService.publicTeachers();
    }

    @GetMapping("/api/public/teachers/{id}")
    public TeacherProfile teacher(@PathVariable Long id) {
        return profileService.publicTeacher(id);
    }
}
