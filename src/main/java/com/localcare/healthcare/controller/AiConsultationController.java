package com.localcare.healthcare.controller;

import com.localcare.healthcare.common.ApiResponse;
import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.AiConsultation;
import com.localcare.healthcare.model.User;
import com.localcare.healthcare.service.AiConsultationService;
import com.localcare.healthcare.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-consultations")
public class AiConsultationController {

    private final AuthService authService;
    private final AiConsultationService aiConsultationService;

    public AiConsultationController(AuthService authService, AiConsultationService aiConsultationService) {
        this.authService = authService;
        this.aiConsultationService = aiConsultationService;
    }

    @PostMapping("/analyze")
    public ApiResponse<AiConsultation> analyze(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @RequestBody Map<String, String> body
    ) {
        try {
            User user = requirePatient(sessionToken);
            String symptoms = body == null ? null : body.get("symptoms");
            return ApiResponse.success(aiConsultationService.analyze(user, symptoms));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/my")
    public ApiResponse<List<AiConsultation>> my(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            User user = requirePatient(sessionToken);
            return ApiResponse.success(aiConsultationService.my(user));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    private User requirePatient(String sessionToken) {
        User user = authService.requireUser(sessionToken);
        if (!"PATIENT".equals(user.getRole())) {
            throw new BusinessException("只有患者可以使用 AI 问诊");
        }
        return user;
    }
}
