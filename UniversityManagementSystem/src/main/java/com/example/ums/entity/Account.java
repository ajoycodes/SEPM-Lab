package com.example.ums.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * One table for everyone who can log in. The role column says whether a row is an admin, a teacher or a
 * student, and fields that apply to only one role are empty for the others.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Also the login name. Unique across all roles. */
    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash. The plain password is never stored. */
    @Column(nullable = false)
    private String passwordHash;

    /** Set from the endpoint that created the record, never from the request body. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String department;

    @Column(length = 40)
    private String phone;

    // Teachers
    @Column(length = 100)
    private String designation;

    @Column(length = 160)
    private String specialization;

    @Column(nullable = false)
    private boolean departmentHead;

    @Column(nullable = false)
    private boolean facultyDean;

    @Column(length = 2000)
    private String bio;

    @Column(length = 1500)
    private String researchInterests;

    @ElementCollection
    @CollectionTable(name = "teacher_education", joinColumns = @JoinColumn(name = "account_id"))
    @OrderColumn(name = "entry_order")
    private List<EducationEntry> education = new ArrayList<>();

    // Students
    @Column(length = 50, unique = true)
    private String roll;

    @Column(length = 150)
    private String program;

    @Column(precision = 3, scale = 2)
    private BigDecimal cgpa;

    @ElementCollection
    @CollectionTable(name = "student_results", joinColumns = @JoinColumn(name = "account_id"))
    @OrderColumn(name = "entry_order")
    private List<SemesterResult> semesterResults = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public boolean isDepartmentHead() {
        return departmentHead;
    }

    public void setDepartmentHead(boolean departmentHead) {
        this.departmentHead = departmentHead;
    }

    public boolean isFacultyDean() {
        return facultyDean;
    }

    public void setFacultyDean(boolean facultyDean) {
        this.facultyDean = facultyDean;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getResearchInterests() {
        return researchInterests;
    }

    public void setResearchInterests(String researchInterests) {
        this.researchInterests = researchInterests;
    }

    public List<EducationEntry> getEducation() {
        return education;
    }

    public String getRoll() {
        return roll;
    }

    public void setRoll(String roll) {
        this.roll = roll;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public BigDecimal getCgpa() {
        return cgpa;
    }

    public void setCgpa(BigDecimal cgpa) {
        this.cgpa = cgpa;
    }

    public List<SemesterResult> getSemesterResults() {
        return semesterResults;
    }
}
