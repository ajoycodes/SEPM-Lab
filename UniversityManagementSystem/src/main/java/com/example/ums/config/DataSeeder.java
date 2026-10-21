package com.example.ums.config;

import com.example.ums.entity.Admin;
import com.example.ums.entity.Student;
import com.example.ums.entity.Teacher;
import com.example.ums.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * On the very first start (empty database) creates one account per role, so there is someone to log in as.
 * Passwords come from application.yml / environment variables and are stored as BCrypt hashes.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String teacherPassword;
    private final String studentPassword;

    public DataSeeder(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                      @Value("${app.seed.admin-password}") String adminPassword,
                      @Value("${app.seed.teacher-password}") String teacherPassword,
                      @Value("${app.seed.student-password}") String studentPassword) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.teacherPassword = teacherPassword;
        this.studentPassword = studentPassword;
    }

    @Override
    public void run(String... args) {
        if (appUserRepository.count() > 0) {
            return;
        }

        Admin admin = new Admin();
        admin.setName("System Admin");
        admin.setEmail("admin@university.edu");
        admin.setPhone("+8801700000000");
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode(adminPassword));

        Teacher teacher = new Teacher();
        teacher.setName("Ayesha Rahman");
        teacher.setEmail("ayesha.rahman@university.edu");
        teacher.setDepartment("CSE");
        teacher.setDesignation("Assistant Professor");
        teacher.setUsername("teacher");
        teacher.setPassword(passwordEncoder.encode(teacherPassword));

        Student student = new Student();
        student.setName("Tanvir Hasan");
        student.setEmail("tanvir.hasan@university.edu");
        student.setRoll("2107001");
        student.setDepartment("CSE");
        student.setBatch(2021);
        student.setUsername("student");
        student.setPassword(passwordEncoder.encode(studentPassword));

        appUserRepository.save(admin);
        appUserRepository.save(teacher);
        appUserRepository.save(student);
        log.info("Created the starting accounts: admin, teacher, student");
    }
}
