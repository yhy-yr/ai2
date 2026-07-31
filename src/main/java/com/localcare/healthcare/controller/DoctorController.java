package com.localcare.healthcare.controller;

import com.localcare.healthcare.common.ApiResponse;
import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.Doctor;
import com.localcare.healthcare.model.User;
import com.localcare.healthcare.service.AuthService;
import com.localcare.healthcare.service.DoctorService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final AuthService authService;
    private final DoctorService doctorService;

    public DoctorController(AuthService authService, DoctorService doctorService) {
        this.authService = authService;
        this.doctorService = doctorService;
    }

    @GetMapping
    public ApiResponse<List<Doctor>> list(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        try {
            authService.requireUser(sessionToken);
            return ApiResponse.success(doctorService.list(keyword));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Doctor> detail(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @PathVariable Long id
    ) {
        try {
            authService.requireUser(sessionToken);
            return ApiResponse.success(doctorService.detail(id));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/by-department/{departmentId}")
    public ApiResponse<List<Doctor>> byDepartment(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @PathVariable Long departmentId
    ) {
        try {
            authService.requireUser(sessionToken);
            return ApiResponse.success(doctorService.byDepartment(departmentId));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<Doctor> create(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @RequestBody Doctor doctor
    ) {
        try {
            requireAdmin(sessionToken);
            return ApiResponse.success(doctorService.create(doctor));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Doctor> update(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @PathVariable Long id,
            @RequestBody Doctor doctor
    ) {
        try {
            requireAdmin(sessionToken);
            return ApiResponse.success(doctorService.update(id, doctor));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @PathVariable Long id
    ) {
        try {
            requireAdmin(sessionToken);
            doctorService.delete(id);
            return ApiResponse.success(null);
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    private void requireAdmin(String sessionToken) {
        User user = authService.requireUser(sessionToken);
        authService.requireRole(user, "ADMIN");
    }
}
