package com.localcare.healthcare.controller;

import com.localcare.healthcare.common.ApiResponse;
import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.Patient;
import com.localcare.healthcare.model.User;
import com.localcare.healthcare.service.AuthService;
import com.localcare.healthcare.service.PatientService;
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
@RequestMapping("/api/patients")
public class PatientController {

    private final AuthService authService;
    private final PatientService patientService;

    public PatientController(AuthService authService, PatientService patientService) {
        this.authService = authService;
        this.patientService = patientService;
    }

    @GetMapping
    public ApiResponse<List<Patient>> list(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        try {
            authService.requireUser(sessionToken);
            return ApiResponse.success(patientService.list(keyword));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Patient> detail(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @PathVariable Long id
    ) {
        try {
            authService.requireUser(sessionToken);
            return ApiResponse.success(patientService.detail(id));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<Patient> create(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @RequestBody Patient patient
    ) {
        try {
            requireAdmin(sessionToken);
            return ApiResponse.success(patientService.create(patient));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Patient> update(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @PathVariable Long id,
            @RequestBody Patient patient
    ) {
        try {
            requireAdmin(sessionToken);
            return ApiResponse.success(patientService.update(id, patient));
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
            patientService.delete(id);
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

