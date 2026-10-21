package com.example.ums.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "teachers")
public class Teacher extends AppUser {

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 50)
    private String designation;

    public Teacher() {
        super(Role.TEACHER);
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}
