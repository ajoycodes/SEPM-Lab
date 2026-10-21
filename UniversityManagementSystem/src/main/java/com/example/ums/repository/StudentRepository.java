package com.example.ums.repository;

import com.example.ums.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByRoll(String roll);

    boolean existsByRollAndIdNot(String roll, Long id);
}
