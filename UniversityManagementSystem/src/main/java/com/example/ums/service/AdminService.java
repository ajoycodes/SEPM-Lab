package com.example.ums.service;

import com.example.ums.dto.AdminRequest;
import com.example.ums.dto.AdminResponse;
import com.example.ums.entity.Admin;
import com.example.ums.exception.ConflictException;
import com.example.ums.exception.ResourceNotFoundException;
import com.example.ums.repository.AdminRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminService {

    private final AdminRepository adminRepository;
    private final AccountService accountService;

    public AdminService(AdminRepository adminRepository, AccountService accountService) {
        this.adminRepository = adminRepository;
        this.accountService = accountService;
    }

    @Transactional(readOnly = true)
    public List<AdminResponse> findAll() {
        return adminRepository.findAll(Sort.by("id")).stream().map(AdminResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AdminResponse findById(Long id) {
        return AdminResponse.from(getAdmin(id));
    }

    public AdminResponse create(AdminRequest request) {
        Admin admin = new Admin();
        copyFields(admin, request);
        return AdminResponse.from(adminRepository.save(admin));
    }

    public AdminResponse update(Long id, AdminRequest request) {
        Admin admin = getAdmin(id);
        copyFields(admin, request);
        return AdminResponse.from(adminRepository.save(admin));
    }

    public void delete(Long id) {
        Admin admin = getAdmin(id);
        // Without an admin nobody could manage admins again.
        if (adminRepository.count() == 1) {
            throw new ConflictException("Cannot delete the last admin");
        }
        adminRepository.delete(admin);
    }

    private Admin getAdmin(Long id) {
        return adminRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Admin", id));
    }

    private void copyFields(Admin admin, AdminRequest request) {
        accountService.applyAccountFields(admin, request.name(), request.email(), request.username(),
                request.password());
        admin.setPhone(request.phone());
    }
}
