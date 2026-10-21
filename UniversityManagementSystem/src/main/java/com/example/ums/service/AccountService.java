package com.example.ums.service;

import com.example.ums.entity.AppUser;
import com.example.ums.exception.ConflictException;
import com.example.ums.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * The login fields every role shares (name, e-mail, username, password).
 * Admin, Teacher and Student services all call this, so the rules live in one place.
 */
@Service
public class AccountService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Checks that the username and e-mail are not used by anyone else (in any role), then copies the fields.
     * The password is hashed with BCrypt. A null password on update keeps the old one.
     */
    public void applyAccountFields(AppUser user, String name, String email, String username, String password) {
        Long id = user.getId();
        boolean usernameTaken = id == null
                ? appUserRepository.existsByUsername(username)
                : appUserRepository.existsByUsernameAndIdNot(username, id);
        if (usernameTaken) {
            throw new ConflictException("Username '" + username + "' is already taken");
        }
        boolean emailTaken = id == null
                ? appUserRepository.existsByEmail(email)
                : appUserRepository.existsByEmailAndIdNot(email, id);
        if (emailTaken) {
            throw new ConflictException("E-mail '" + email + "' is already used");
        }

        user.setName(name);
        user.setEmail(email);
        user.setUsername(username);
        if (password != null) {
            user.setPassword(passwordEncoder.encode(password));
        }
    }
}
