package com.localcare.healthcare.controller;

import com.localcare.healthcare.common.ApiResponse;
import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.Medicine;
import com.localcare.healthcare.model.User;
import com.localcare.healthcare.service.AuthService;
import com.localcare.healthcare.service.MedicineService;
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
@RequestMapping("/api/medicines")
public class MedicineController {

    private final AuthService authService;
    private final MedicineService medicineService;

    public MedicineController(AuthService authService, MedicineService medicineService) {
        this.authService = authService;
        this.medicineService = medicineService;
    }

    @GetMapping
    public ApiResponse<List<Medicine>> list(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        try {
            authService.requireUser(sessionToken);
            return ApiResponse.success(medicineService.list(keyword));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<Medicine> create(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @RequestBody Medicine medicine
    ) {
        try {
            requireAdmin(sessionToken);
            return ApiResponse.success(medicineService.create(medicine));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Medicine> update(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @PathVariable Long id,
            @RequestBody Medicine medicine
    ) {
        try {
            requireAdmin(sessionToken);
            return ApiResponse.success(medicineService.update(id, medicine));
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
            medicineService.delete(id);
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

