package com.localcare.healthcare.controller;

import com.localcare.healthcare.common.ApiResponse;
import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        try {
            return ApiResponse.success(authService.login(body.get("username"), body.get("password")));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        try {
            return ApiResponse.success(authService.register(body));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            return ApiResponse.success(authService.currentUserInfo(sessionToken));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        try {
            authService.requireUser(sessionToken);
            authService.logout(sessionToken);
            return ApiResponse.success(null);
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
