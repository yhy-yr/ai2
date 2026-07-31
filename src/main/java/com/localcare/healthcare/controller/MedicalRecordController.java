package com.localcare.healthcare.controller;

import com.localcare.healthcare.common.ApiResponse;
import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.MedicalRecord;
import com.localcare.healthcare.model.User;
import com.localcare.healthcare.service.AuthService;
import com.localcare.healthcare.service.MedicalRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    private final AuthService authService;
    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(AuthService authService, MedicalRecordService medicalRecordService) {
        this.authService = authService;
        this.medicalRecordService = medicalRecordService;
    }

    @PostMapping("/ai-draft/{appointmentId}")
    public ApiResponse<MedicalRecord> aiDraft(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @PathVariable Long appointmentId
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "DOCTOR");
            return ApiResponse.success(medicalRecordService.aiDraft(user, appointmentId));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<MedicalRecord> save(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @RequestBody MedicalRecord record
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "DOCTOR");
            return ApiResponse.success(medicalRecordService.save(user, record));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<MedicalRecord> update(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @PathVariable Long id,
            @RequestBody MedicalRecord record
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "DOCTOR");
            return ApiResponse.success(medicalRecordService.update(user, id, record));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/doctor")
    public ApiResponse<List<MedicalRecord>> doctorList(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "DOCTOR");
            return ApiResponse.success(medicalRecordService.doctorList(user));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/my")
    public ApiResponse<List<MedicalRecord>> my(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "PATIENT");
            return ApiResponse.success(medicalRecordService.patientList(user));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<List<MedicalRecord>> list(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "ADMIN");
            return ApiResponse.success(medicalRecordService.listAll(user));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<MedicalRecord> detail(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @PathVariable Long id
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            return ApiResponse.success(medicalRecordService.detail(user, id));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
