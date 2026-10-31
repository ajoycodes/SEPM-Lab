package com.example.ums.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/** One semester's GPA for a student, stored in the student_results table. */
@Embeddable
public class SemesterResult {

    @Column(length = 100)
    private String semester;

    @Column(precision = 3, scale = 2)
    private BigDecimal gpa;

    protected SemesterResult() {
    }

    public SemesterResult(String semester, BigDecimal gpa) {
        this.semester = semester;
        this.gpa = gpa;
    }

    public String getSemester() {
        return semester;
    }

    public BigDecimal getGpa() {
        return gpa;
    }
}
