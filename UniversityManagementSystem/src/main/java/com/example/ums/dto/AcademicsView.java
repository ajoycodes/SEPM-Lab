package com.example.ums.dto;

import java.util.List;

/** The faculties and departments, with their current dean or head and how many people are in each. */
public record AcademicsView(List<FacultyView> faculties) {

    public record FacultyView(String code, String name, Leader dean, List<DepartmentView> departments) {
    }

    public record DepartmentView(String code, String name, Leader head, long teachers, long students) {
    }

    public record Leader(Long id, String name, String email) {
    }
}
