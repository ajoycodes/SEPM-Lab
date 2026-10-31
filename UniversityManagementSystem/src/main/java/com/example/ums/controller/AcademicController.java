package com.example.ums.controller;

import com.example.ums.dto.AcademicsView;
import com.example.ums.service.AcademicCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AcademicController {

    private final AcademicCatalog academicCatalog;

    public AcademicController(AcademicCatalog academicCatalog) {
        this.academicCatalog = academicCatalog;
    }

    @GetMapping("/api/academics")
    public AcademicsView academics() {
        return academicCatalog.view();
    }
}
