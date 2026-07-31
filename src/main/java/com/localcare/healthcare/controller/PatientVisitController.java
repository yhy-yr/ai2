package com.localcare.healthcare.controller;

import com.localcare.healthcare.common.ApiResponse;
import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.User;
import com.localcare.healthcare.service.AuthService;
import com.localcare.healthcare.service.PatientVisitProgressService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/patient")
public class PatientVisitController {

    private final AuthService authService;
    private final PatientVisitProgressService patientVisitProgressService;

    public PatientVisitController(AuthService authService, PatientVisitProgressService patientVisitProgressService) {
        this.authService = authService;
        this.patientVisitProgressService = patientVisitProgressService;
    }

    @GetMapping("/visitProgress")
    public ApiResponse<Map<String, Object>> visitProgress(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @RequestParam Long appointmentId
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "PATIENT");
            return ApiResponse.success(patientVisitProgressService.getProgress(user, appointmentId));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
