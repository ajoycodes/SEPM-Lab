package com.example.ums.service;

import com.example.ums.dto.StudentRequest;
import com.example.ums.dto.StudentResponse;
import com.example.ums.entity.Student;
import com.example.ums.exception.ConflictException;
import com.example.ums.exception.ResourceNotFoundException;
import com.example.ums.repository.StudentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final AccountService accountService;

    public StudentService(StudentRepository studentRepository, AccountService accountService) {
        this.studentRepository = studentRepository;
        this.accountService = accountService;
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> findAll() {
        return studentRepository.findAll(Sort.by("id")).stream().map(StudentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(Long id) {
        return StudentResponse.from(getStudent(id));
    }

    public StudentResponse create(StudentRequest request) {
        Student student = new Student();
        copyFields(student, request);
        return StudentResponse.from(studentRepository.save(student));
    }

    public StudentResponse update(Long id, StudentRequest request) {
        Student student = getStudent(id);
        copyFields(student, request);
        return StudentResponse.from(studentRepository.save(student));
    }

    public void delete(Long id) {
        studentRepository.delete(getStudent(id));
    }

    private Student getStudent(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }

    private void copyFields(Student student, StudentRequest request) {
        boolean rollTaken = student.getId() == null
                ? studentRepository.existsByRoll(request.roll())
                : studentRepository.existsByRollAndIdNot(request.roll(), student.getId());
        if (rollTaken) {
            throw new ConflictException("Roll '" + request.roll() + "' is already used");
        }
        accountService.applyAccountFields(student, request.name(), request.email(), request.username(),
                request.password());
        student.setRoll(request.roll());
        student.setDepartment(request.department());
        student.setBatch(request.batch());
    }
}
