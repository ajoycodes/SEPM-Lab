package com.example.ums.service;

import com.example.ums.dto.AccountRequest;
import com.example.ums.dto.AccountResponse;
import com.example.ums.entity.Account;
import com.example.ums.entity.Role;
import com.example.ums.repository.AccountRepository;
import com.example.ums.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Create, read, update and delete for admins, teachers and students. All three share this code and one table. */
@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AcademicCatalog academicCatalog;
    private final CurrentUser currentUser;

    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder,
                          AcademicCatalog academicCatalog, CurrentUser currentUser) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.academicCatalog = academicCatalog;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list(Role role) {
        return accountRepository.findByRoleOrderByIdAsc(role).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(Role role, Long id) {
        return toResponse(find(role, id));
    }

    public AccountResponse create(Role role, AccountRequest request) {
        if (request.password() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        Account account = new Account();
        account.setRole(role);
        return save(account, request);
    }

    public AccountResponse update(Role role, Long id, AccountRequest request) {
        return save(find(role, id), request);
    }

    public void delete(Role role, Long id) {
        accountRepository.delete(find(role, id));
    }

    public AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getId(), account.getName(), account.getEmail(), account.getDepartment(),
                account.getRole(), academicCatalog.facultyName(account.getDepartment()), account.getDesignation(),
                account.getSpecialization(), account.isDepartmentHead(), account.isFacultyDean());
    }

    private Account find(Role role, Long id) {
        return accountRepository.findByIdAndRole(id, role)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));
    }

    private AccountResponse save(Account account, AccountRequest request) {
        String department = academicCatalog.departmentName(request.department().strip());
        applyLeadership(account, department, request);

        String email = request.email().strip().toLowerCase(Locale.ROOT);
        accountRepository.findByEmail(email)
                .filter(other -> !Objects.equals(other.getId(), account.getId()))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
                });

        if (request.password() != null) {
            // BCrypt only reads the first 72 bytes, and a non-English character can take several.
            if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 bytes");
            }
            account.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        account.setName(request.name().strip());
        account.setEmail(email);
        account.setDepartment(department);
        if (account.getRole() == Role.TEACHER) {
            if (request.designation() != null) {
                account.setDesignation(request.designation().strip());
            }
            if (request.specialization() != null) {
                account.setSpecialization(request.specialization().strip());
            }
        }
        return toResponse(accountRepository.saveAndFlush(account));
    }

    /** Only admins appoint a department head or faculty dean, and each post can have one holder. */
    private void applyLeadership(Account account, String department, AccountRequest request) {
        boolean head = request.departmentHead() == null ? account.isDepartmentHead() : request.departmentHead();
        boolean dean = request.facultyDean() == null ? account.isFacultyDean() : request.facultyDean();
        boolean changed = head != account.isDepartmentHead() || dean != account.isFacultyDean()
                || ((head || dean) && !department.equals(account.getDepartment()));
        if (changed && !currentUser.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only admins can appoint department heads and faculty deans");
        }
        if (head || dean) {
            String faculty = academicCatalog.facultyName(department);
            if (account.getRole() != Role.TEACHER || faculty.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A head or dean must be a teacher in a listed department");
            }
            for (Account other : accountRepository.findByRoleOrderByIdAsc(Role.TEACHER)) {
                if (Objects.equals(other.getId(), account.getId())) {
                    continue;
                }
                if (head && other.isDepartmentHead() && academicCatalog.departmentName(other.getDepartment()).equals(department)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "This department already has a head");
                }
                if (dean && other.isFacultyDean() && academicCatalog.facultyName(other.getDepartment()).equals(faculty)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "This faculty already has a dean");
                }
            }
        }
        account.setDepartmentHead(head);
        account.setFacultyDean(dean);
    }
}
