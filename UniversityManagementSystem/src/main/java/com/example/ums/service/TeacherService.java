package com.example.ums.service;

import com.example.ums.dto.TeacherRequest;
import com.example.ums.dto.TeacherResponse;
import com.example.ums.entity.Teacher;
import com.example.ums.exception.ResourceNotFoundException;
import com.example.ums.repository.TeacherRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final AccountService accountService;

    public TeacherService(TeacherRepository teacherRepository, AccountService accountService) {
        this.teacherRepository = teacherRepository;
        this.accountService = accountService;
    }

    @Transactional(readOnly = true)
    public List<TeacherResponse> findAll() {
        return teacherRepository.findAll(Sort.by("id")).stream().map(TeacherResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TeacherResponse findById(Long id) {
        return TeacherResponse.from(getTeacher(id));
    }

    public TeacherResponse create(TeacherRequest request) {
        Teacher teacher = new Teacher();
        copyFields(teacher, request);
        return TeacherResponse.from(teacherRepository.save(teacher));
    }

    public TeacherResponse update(Long id, TeacherRequest request) {
        Teacher teacher = getTeacher(id);
        copyFields(teacher, request);
        return TeacherResponse.from(teacherRepository.save(teacher));
    }

    public void delete(Long id) {
        teacherRepository.delete(getTeacher(id));
    }

    private Teacher getTeacher(Long id) {
        return teacherRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Teacher", id));
    }

    private void copyFields(Teacher teacher, TeacherRequest request) {
        accountService.applyAccountFields(teacher, request.name(), request.email(), request.username(),
                request.password());
        teacher.setDepartment(request.department());
        teacher.setDesignation(request.designation());
    }
}
