package com.localcare.healthcare.controller;

import com.localcare.healthcare.common.ApiResponse;
import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.Appointment;
import com.localcare.healthcare.model.User;
import com.localcare.healthcare.service.AppointmentService;
import com.localcare.healthcare.service.AuthService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AuthService authService;
    private final AppointmentService appointmentService;

    public AppointmentController(AuthService authService, AppointmentService appointmentService) {
        this.authService = authService;
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ApiResponse<Appointment> create(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @RequestBody Appointment appointment
    ) {
        try {
            User user = requirePatient(sessionToken);
            return ApiResponse.success(appointmentService.create(user, appointment));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/my")
    public ApiResponse<List<Appointment>> my(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            User user = requirePatient(sessionToken);
            return ApiResponse.success(appointmentService.my(user));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancel(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @PathVariable Long id
    ) {
        try {
            User user = requirePatient(sessionToken);
            appointmentService.cancel(user, id);
            return ApiResponse.success(null);
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/doctor")
    public ApiResponse<List<Appointment>> doctorList(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "DOCTOR");
            return ApiResponse.success(appointmentService.doctorList(user));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Appointment> updateStatus(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            String status = body == null ? null : body.get("status");
            return ApiResponse.success(appointmentService.updateStatus(user, id, status));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<List<Appointment>> list(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "ADMIN");
            return ApiResponse.success(appointmentService.listAll(user));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    private User requirePatient(String sessionToken) {
        User user = authService.requireUser(sessionToken);
        if (!"PATIENT".equals(user.getRole())) {
            throw new BusinessException("只有患者可以预约挂号");
        }
        return user;
    }
}
