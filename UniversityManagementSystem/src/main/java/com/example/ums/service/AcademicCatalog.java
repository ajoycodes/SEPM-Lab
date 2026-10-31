package com.example.ums.service;

import com.example.ums.dto.AcademicsView;
import com.example.ums.dto.AcademicsView.DepartmentView;
import com.example.ums.dto.AcademicsView.FacultyView;
import com.example.ums.dto.AcademicsView.Leader;
import com.example.ums.entity.Account;
import com.example.ums.entity.Role;
import com.example.ums.repository.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** The faculties and departments listed in academics.json, based on KUET's structure. */
@Service
public class AcademicCatalog {

    private record Department(String code, String name, List<String> specializations) {
    }

    private record Faculty(String code, String name, List<Department> departments) {
    }

    private record Structure(List<Faculty> faculties) {
    }

    private final AccountRepository accountRepository;
    private final Structure structure;

    public AcademicCatalog(AccountRepository accountRepository, ObjectMapper objectMapper) throws IOException {
        this.accountRepository = accountRepository;
        try (InputStream input = getClass().getResourceAsStream("/academics.json")) {
            this.structure = objectMapper.readValue(input, Structure.class);
        }
    }

    /** Turns a code or name ("CSE") into the full department name. Anything not listed comes back unchanged. */
    public String departmentName(String value) {
        for (Faculty faculty : structure.faculties()) {
            for (Department department : faculty.departments()) {
                if (department.code().equalsIgnoreCase(value) || department.name().equalsIgnoreCase(value)) {
                    return department.name();
                }
            }
        }
        return value;
    }

    /** The faculty a department belongs to, or an empty string if the department isn't listed. */
    public String facultyName(String department) {
        String name = departmentName(department);
        for (Faculty faculty : structure.faculties()) {
            for (Department candidate : faculty.departments()) {
                if (candidate.name().equals(name)) {
                    return faculty.name();
                }
            }
        }
        return "";
    }

    public AcademicsView view() {
        List<Account> teachers = accountRepository.findByRoleOrderByIdAsc(Role.TEACHER);
        List<Account> students = accountRepository.findByRoleOrderByIdAsc(Role.STUDENT);
        return new AcademicsView(structure.faculties().stream()
                .map(faculty -> facultyView(faculty, teachers, students))
                .toList());
    }

    private FacultyView facultyView(Faculty faculty, List<Account> teachers, List<Account> students) {
        Leader dean = teachers.stream()
                .filter(teacher -> teacher.isFacultyDean() && facultyName(teacher.getDepartment()).equals(faculty.name()))
                .findFirst().map(this::leader).orElse(null);
        List<DepartmentView> departments = faculty.departments().stream()
                .map(department -> departmentView(department, teachers, students))
                .toList();
        return new FacultyView(faculty.code(), faculty.name(), dean, departments);
    }

    private DepartmentView departmentView(Department department, List<Account> teachers, List<Account> students) {
        List<Account> staff = teachers.stream().filter(teacher -> inDepartment(teacher, department)).toList();
        Leader head = staff.stream().filter(Account::isDepartmentHead).findFirst().map(this::leader).orElse(null);
        long studentCount = students.stream().filter(student -> inDepartment(student, department)).count();
        return new DepartmentView(department.code(), department.name(), head, staff.size(), studentCount);
    }

    private boolean inDepartment(Account account, Department department) {
        return departmentName(account.getDepartment()).equals(department.name());
    }

    private Leader leader(Account account) {
        return new Leader(account.getId(), account.getName(), account.getEmail());
    }
}
