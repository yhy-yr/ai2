package com.localcare.healthcare.controller;

import com.localcare.healthcare.common.ApiResponse;
import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.Prescription;
import com.localcare.healthcare.model.User;
import com.localcare.healthcare.service.AuthService;
import com.localcare.healthcare.service.PrescriptionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final AuthService authService;
    private final PrescriptionService prescriptionService;

    public PrescriptionController(AuthService authService, PrescriptionService prescriptionService) {
        this.authService = authService;
        this.prescriptionService = prescriptionService;
    }

    @PostMapping
    public ApiResponse<Prescription> create(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @RequestBody Prescription prescription
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "DOCTOR");
            return ApiResponse.success(prescriptionService.create(user, prescription));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/doctor")
    public ApiResponse<List<Prescription>> doctorList(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "DOCTOR");
            return ApiResponse.success(prescriptionService.doctorList(user));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/my")
    public ApiResponse<List<Prescription>> my(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "PATIENT");
            return ApiResponse.success(prescriptionService.patientList(user));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<List<Prescription>> list(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "ADMIN");
            return ApiResponse.success(prescriptionService.listAll(user));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Prescription> detail(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @PathVariable Long id
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            return ApiResponse.success(prescriptionService.detail(user, id));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}

