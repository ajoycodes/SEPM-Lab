package com.example.ums.repository;

import com.example.ums.entity.Account;
import com.example.ums.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByEmail(String email);

    Optional<Account> findByRoll(String roll);

    Optional<Account> findByIdAndRole(Long id, Role role);

    List<Account> findByRoleOrderByIdAsc(Role role);
}
