package com.example.ums.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "students")
public class Student extends AppUser {

    @Column(nullable = false, unique = true, length = 20)
    private String roll;

    @Column(nullable = false, length = 100)
    private String department;

    /** Year of admission, e.g. 2021. */
    @Column(nullable = false)
    private Integer batch;

    public Student() {
        super(Role.STUDENT);
    }

    public String getRoll() {
        return roll;
    }

    public void setRoll(String roll) {
        this.roll = roll;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getBatch() {
        return batch;
    }

    public void setBatch(Integer batch) {
        this.batch = batch;
    }
}
