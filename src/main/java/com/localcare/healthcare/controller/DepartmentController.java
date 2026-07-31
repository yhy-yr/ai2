package com.localcare.healthcare.controller;

import com.localcare.healthcare.common.ApiResponse;
import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.Department;
import com.localcare.healthcare.model.User;
import com.localcare.healthcare.service.AuthService;
import com.localcare.healthcare.service.DepartmentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final AuthService authService;
    private final DepartmentService departmentService;

    public DepartmentController(AuthService authService, DepartmentService departmentService) {
        this.authService = authService;
        this.departmentService = departmentService;
    }

    @GetMapping
    public ApiResponse<List<Department>> list(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        try {
            authService.requireUser(sessionToken);
            return ApiResponse.success(departmentService.list(keyword));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<Department> create(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @RequestBody Department department
    ) {
        try {
            requireAdmin(sessionToken);
            return ApiResponse.success(departmentService.create(department));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Department> update(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @PathVariable Long id,
            @RequestBody Department department
    ) {
        try {
            requireAdmin(sessionToken);
            return ApiResponse.success(departmentService.update(id, department));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
            @PathVariable Long id
    ) {
        try {
            requireAdmin(sessionToken);
            departmentService.delete(id);
            return ApiResponse.success(null);
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    private void requireAdmin(String sessionToken) {
        User user = authService.requireUser(sessionToken);
        authService.requireRole(user, "ADMIN");
    }
}

