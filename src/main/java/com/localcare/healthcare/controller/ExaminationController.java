package com.localcare.healthcare.controller;

import com.localcare.healthcare.common.ApiResponse;
import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.Examination;
import com.localcare.healthcare.model.User;
import com.localcare.healthcare.service.AuthService;
import com.localcare.healthcare.service.ExaminationService;
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
@RequestMapping("/api/examinations")
public class ExaminationController {

    private final AuthService authService;
    private final ExaminationService examinationService;

    public ExaminationController(AuthService authService, ExaminationService examinationService) {
        this.authService = authService;
        this.examinationService = examinationService;
    }

    @PostMapping
    public ApiResponse<Examination> create(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @RequestBody Examination examination
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "DOCTOR");
            return ApiResponse.success(examinationService.create(user, examination));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Examination> update(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @PathVariable Long id,
            @RequestBody Examination examination
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "DOCTOR");
            return ApiResponse.success(examinationService.update(user, id, examination));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/doctor")
    public ApiResponse<List<Examination>> doctorList(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "DOCTOR");
            return ApiResponse.success(examinationService.doctorList(user));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/my")
    public ApiResponse<List<Examination>> my(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "PATIENT");
            return ApiResponse.success(examinationService.patientList(user));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<List<Examination>> list(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            User user = authService.requireUser(sessionToken);
            authService.requireRole(user, "ADMIN");
            return ApiResponse.success(examinationService.listAll(user));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
