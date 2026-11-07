package com.example.ums.unit;

import com.example.ums.dto.AccountRequest;
import com.example.ums.dto.AccountResponse;
import com.example.ums.entity.Account;
import com.example.ums.entity.Role;
import com.example.ums.repository.AccountRepository;
import com.example.ums.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests: AccountService on its own. The repository and the password encoder are Mockito mocks,
 * so no Spring, no database and no web server are started.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceUnitTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AccountService accountService;

    private static AccountRequest request(String email, String password) {
        return new AccountRequest("Nusrat Jahan", email, password, "CSE", null, null);
    }

    /** An account that is already in the database. */
    private static Account savedStudent(long id, String email) {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", id);
        account.setRole(Role.STUDENT);
        account.setName("Old Name");
        account.setEmail(email);
        account.setDepartment("CSE");
        account.setPasswordHash("old-hash");
        return account;
    }

    /** Makes saveAndFlush hand back the account it was given, as the real repository does. */
    private void saveReturnsItsArgument() {
        when(accountRepository.saveAndFlush(any(Account.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void createSavesTheRoleAndAHashedPassword() {
        when(passwordEncoder.encode("Student123!")).thenReturn("bcrypt-hash");
        saveReturnsItsArgument();

        AccountResponse response = accountService.create(Role.STUDENT, request(" Nusrat@University.EDU ", "Student123!"));

        ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.STUDENT);
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(saved.getValue().getEmail()).isEqualTo("nusrat@university.edu");
        assertThat(response.role()).isEqualTo(Role.STUDENT);
    }

    @Test
    void createWithoutAPasswordIsRejected() {
        assertThatThrownBy(() -> accountService.create(Role.STUDENT, request("nusrat@university.edu", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void aUsedEmailIsAConflict() {
        when(accountRepository.findByEmail("taken@university.edu"))
                .thenReturn(Optional.of(savedStudent(7L, "taken@university.edu")));

        assertThatThrownBy(() -> accountService.create(Role.TEACHER, request("taken@university.edu", "Teacher123!")))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.CONFLICT);
        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateWithoutAPasswordKeepsTheOldOne() {
        Account student = savedStudent(5L, "nusrat@university.edu");
        when(accountRepository.findByIdAndRole(5L, Role.STUDENT)).thenReturn(Optional.of(student));
        saveReturnsItsArgument();

        AccountResponse response = accountService.update(Role.STUDENT, 5L, request("nusrat@university.edu", null));

        assertThat(response.name()).isEqualTo("Nusrat Jahan");
        assertThat(student.getPasswordHash()).isEqualTo("old-hash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void anIdFromAnotherRoleIsNotFound() {
        // Account 5 is a student, so looking it up as a teacher finds nothing.
        when(accountRepository.findByIdAndRole(5L, Role.TEACHER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.get(Role.TEACHER, 5L))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteRemovesTheAccountItFound() {
        Account student = savedStudent(5L, "nusrat@university.edu");
        when(accountRepository.findByIdAndRole(5L, Role.STUDENT)).thenReturn(Optional.of(student));

        accountService.delete(Role.STUDENT, 5L);

        verify(accountRepository).delete(student);
    }
}
