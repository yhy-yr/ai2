package com.localcare.healthcare.controller;

import com.localcare.healthcare.common.ApiResponse;
import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.User;
import com.localcare.healthcare.service.AuthService;
import com.localcare.healthcare.service.DoctorVisitCompletenessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/doctor")
public class DoctorVisitController {

    private final AuthService authService;
    private final DoctorVisitCompletenessService doctorVisitCompletenessService;

    public DoctorVisitController(AuthService authService, DoctorVisitCompletenessService doctorVisitCompletenessService) {
        this.authService = authService;
        this.doctorVisitCompletenessService = doctorVisitCompletenessService;
    }

    @GetMapping("/visit-completeness")
    public ApiResponse<Map<String, Object>> visitCompleteness(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @RequestParam Long appointmentId
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "DOCTOR");
            return ApiResponse.success(doctorVisitCompletenessService.getCompleteness(user, appointmentId));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
