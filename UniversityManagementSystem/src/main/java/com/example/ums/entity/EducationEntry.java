package com.example.ums.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** One line of a teacher's education history, stored in the teacher_education table. */
@Embeddable
public class EducationEntry {

    @Column(length = 160)
    private String qualification;

    @Column(length = 200)
    private String institution;

    private Integer startYear;

    private Integer endYear;

    protected EducationEntry() {
    }

    public EducationEntry(String qualification, String institution, Integer startYear, Integer endYear) {
        this.qualification = qualification;
        this.institution = institution;
        this.startYear = startYear;
        this.endYear = endYear;
    }

    public String getQualification() {
        return qualification;
    }

    public String getInstitution() {
        return institution;
    }

    public Integer getStartYear() {
        return startYear;
    }

    public Integer getEndYear() {
        return endYear;
    }
}
