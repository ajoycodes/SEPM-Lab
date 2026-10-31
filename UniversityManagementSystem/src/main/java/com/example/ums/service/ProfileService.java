package com.example.ums.service;

import com.example.ums.dto.AdminProfile;
import com.example.ums.dto.EducationDto;
import com.example.ums.dto.ProfileRequest;
import com.example.ums.dto.SemesterResultDto;
import com.example.ums.dto.StudentProfile;
import com.example.ums.dto.TeacherProfile;
import com.example.ums.entity.Account;
import com.example.ums.entity.EducationEntry;
import com.example.ums.entity.Role;
import com.example.ums.entity.SemesterResult;
import com.example.ums.repository.AccountRepository;
import com.example.ums.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Personal profiles. Teacher profiles are public; everyone can only read and edit their own full profile. */
@Service
@Transactional
public class ProfileService {

    private final AccountRepository accountRepository;
    private final AcademicCatalog academicCatalog;
    private final CurrentUser currentUser;

    public ProfileService(AccountRepository accountRepository, AcademicCatalog academicCatalog,
                          CurrentUser currentUser) {
        this.accountRepository = accountRepository;
        this.academicCatalog = academicCatalog;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<TeacherProfile> publicTeachers() {
        return accountRepository.findByRoleOrderByIdAsc(Role.TEACHER).stream().map(this::teacherProfile).toList();
    }

    @Transactional(readOnly = true)
    public TeacherProfile publicTeacher(Long id) {
        return accountRepository.findByIdAndRole(id, Role.TEACHER).map(this::teacherProfile)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
    }

    @Transactional(readOnly = true)
    public Object own() {
        return profile(currentUser.require());
    }

    public Object updateOwn(ProfileRequest request) {
        Account account = currentUser.require();

        String email = request.email().strip().toLowerCase(Locale.ROOT);
        accountRepository.findByEmail(email)
                .filter(other -> !other.getId().equals(account.getId()))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
                });
        account.setName(request.name().strip());
        account.setEmail(email);
        account.setPhone(clean(request.phone()));

        if (account.getRole() == Role.TEACHER) {
            updateTeacherFields(account, request);
        } else if (account.getRole() == Role.STUDENT) {
            updateStudentFields(account, request);
        }
        return profile(accountRepository.saveAndFlush(account));
    }

    private void updateTeacherFields(Account account, ProfileRequest request) {
        account.setBio(clean(request.bio()));
        account.setSpecialization(clean(request.researchField()));
        account.setResearchInterests(clean(request.researchInterests()));
        if (request.education() != null) {
            for (EducationDto entry : request.education()) {
                if (entry.startYear() != null && entry.endYear() != null && entry.endYear() < entry.startYear()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Education end year must be after the start year");
                }
            }
            account.getEducation().clear();
            request.education().forEach(entry -> account.getEducation().add(new EducationEntry(
                    entry.qualification().strip(), entry.institution().strip(), entry.startYear(), entry.endYear())));
        }
    }

    private void updateStudentFields(Account account, ProfileRequest request) {
        String roll = clean(request.roll());
        if (roll != null && roll.isBlank()) {
            roll = null;
        }
        if (roll != null) {
            accountRepository.findByRoll(roll)
                    .filter(other -> !other.getId().equals(account.getId()))
                    .ifPresent(other -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Student roll already exists");
                    });
        }
        account.setRoll(roll);

        if (request.department() != null) {
            String department = academicCatalog.departmentName(request.department().strip());
            if (academicCatalog.facultyName(department).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a listed department");
            }
            account.setDepartment(department);
        }
        account.setProgram(clean(request.program()));
        account.setCgpa(request.cgpa());

        if (request.semesterResults() != null) {
            Set<String> seen = new HashSet<>();
            for (SemesterResultDto result : request.semesterResults()) {
                if (!seen.add(result.semester().strip().toLowerCase(Locale.ROOT))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each semester may be listed only once");
                }
            }
            account.getSemesterResults().clear();
            request.semesterResults().forEach(result -> account.getSemesterResults()
                    .add(new SemesterResult(result.semester().strip(), result.gpa())));
        }
    }

    private Object profile(Account account) {
        return switch (account.getRole()) {
            case TEACHER -> teacherProfile(account);
            case STUDENT -> studentProfile(account);
            case ADMIN -> new AdminProfile(account.getId(), account.getName(), account.getEmail(), account.getPhone());
        };
    }

    private TeacherProfile teacherProfile(Account account) {
        List<EducationDto> education = account.getEducation().stream()
                .map(entry -> new EducationDto(entry.getQualification(), entry.getInstitution(),
                        entry.getStartYear(), entry.getEndYear()))
                .toList();
        return new TeacherProfile(account.getId(), account.getName(), account.getEmail(), account.getPhone(),
                account.getDepartment(), academicCatalog.facultyName(account.getDepartment()),
                account.getDesignation(), account.isDepartmentHead(), account.isFacultyDean(), account.getBio(),
                education, account.getSpecialization(), account.getResearchInterests());
    }

    private StudentProfile studentProfile(Account account) {
        List<SemesterResultDto> results = account.getSemesterResults().stream()
                .map(result -> new SemesterResultDto(result.getSemester(), result.getGpa()))
                .toList();
        return new StudentProfile(account.getId(), account.getName(), account.getEmail(), account.getPhone(),
                account.getRoll(), account.getDepartment(), academicCatalog.facultyName(account.getDepartment()),
                account.getProgram(), account.getCgpa(), results);
    }

    private static String clean(String value) {
        return value == null ? null : value.strip();
    }
}
