package com.localcare.healthcare.controller;

import com.localcare.healthcare.common.ApiResponse;
import com.localcare.healthcare.model.RiskAssessment;
import com.localcare.healthcare.service.DeepSeekService;
import com.localcare.healthcare.service.RiskAssessmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiStatusController {

    private final DeepSeekService deepSeekService;
    private final RiskAssessmentService riskAssessmentService;

    public AiStatusController(DeepSeekService deepSeekService, RiskAssessmentService riskAssessmentService) {
        this.deepSeekService = deepSeekService;
        this.riskAssessmentService = riskAssessmentService;
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        boolean online = deepSeekService.isAvailable();
        return ApiResponse.success(Map.of(
                "enabled", online,
                "provider", "DeepSeek",
                "mode", online ? "online" : "local-fallback",
                "message", online ? "DeepSeek 已连接" : "本地规则模式"
        ));
    }

    @GetMapping("/risk")
    public ApiResponse<RiskAssessment> risk(@RequestParam(value = "symptoms", required = false) String symptoms) {
        return ApiResponse.success(riskAssessmentService.assess(symptoms));
    }

    @PostMapping("/risk")
    public ApiResponse<RiskAssessment> risk(@RequestBody(required = false) Map<String, String> body) {
        String symptoms = body == null ? "" : body.get("symptoms");
        return ApiResponse.success(riskAssessmentService.assess(symptoms));
    }
}
