package com.localcare.healthcare.controller;

import com.localcare.healthcare.common.ApiResponse;
import com.localcare.healthcare.common.AuthContext;
import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.User;
import com.localcare.healthcare.service.AuthService;
import com.localcare.healthcare.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AuthService authService;
    private final DashboardService dashboardService;

    public DashboardController(AuthService authService, DashboardService dashboardService) {
        this.authService = authService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin")
    public ApiResponse<Map<String, Object>> admin(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            AuthContext.setCurrentUser(user);
            authService.requireRole(user, "ADMIN");
            return ApiResponse.success(dashboardService.adminStats());
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        } finally {
            AuthContext.clear();
        }
    }

    @GetMapping("/doctor")
    public ApiResponse<Map<String, Object>> doctor(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            AuthContext.setCurrentUser(user);
            authService.requireRole(user, "DOCTOR");
            return ApiResponse.success(dashboardService.doctorStats(user));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        } finally {
            AuthContext.clear();
        }
    }

    @GetMapping("/patient")
    public ApiResponse<Map<String, Object>> patient(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            AuthContext.setCurrentUser(user);
            authService.requireRole(user, "PATIENT");
            return ApiResponse.success(dashboardService.patientStats(user));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        } finally {
            AuthContext.clear();
        }
    }
}
