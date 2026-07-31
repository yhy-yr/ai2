package com.localcare.healthcare.service;

import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.Doctor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Service
public class DoctorService {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Doctor> rowMapper = (rs, rowNum) -> {
        Doctor doctor = new Doctor();
        doctor.setId(rs.getLong("id"));
        doctor.setUserId(rs.getLong("user_id"));
        doctor.setUsername(rs.getString("username"));
        doctor.setRealName(rs.getString("real_name"));
        doctor.setPhone(rs.getString("phone"));
        doctor.setStatus(rs.getString("status"));
        doctor.setDepartmentId(rs.getLong("department_id"));
        doctor.setDepartmentName(rs.getString("department_name"));
        doctor.setTitle(rs.getString("title"));
        doctor.setSpecialty(rs.getString("specialty"));
        doctor.setIntroduction(rs.getString("introduction"));
        return doctor;
    };

    public DoctorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Doctor> list(String keyword) {
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            return jdbcTemplate.query("""
                    SELECT d.*, u.username, u.real_name, u.phone, u.status, dep.name AS department_name
                    FROM doctors d
                    JOIN users u ON u.id = d.user_id
                    LEFT JOIN departments dep ON dep.id = d.department_id
                    WHERE u.username LIKE ? OR u.real_name LIKE ? OR u.phone LIKE ? OR dep.name LIKE ? OR d.specialty LIKE ?
                    ORDER BY d.id ASC
                    """, rowMapper, like, like, like, like, like);
        }

        return jdbcTemplate.query("""
                SELECT d.*, u.username, u.real_name, u.phone, u.status, dep.name AS department_name
                FROM doctors d
                JOIN users u ON u.id = d.user_id
                LEFT JOIN departments dep ON dep.id = d.department_id
                ORDER BY d.id ASC
                """, rowMapper);
    }

    public Doctor detail(Long id) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT d.*, u.username, u.real_name, u.phone, u.status, dep.name AS department_name
                    FROM doctors d
                    JOIN users u ON u.id = d.user_id
                    LEFT JOIN departments dep ON dep.id = d.department_id
                    WHERE d.id = ?
                    """, rowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("医生不存在");
        }
    }

    public List<Doctor> byDepartment(Long departmentId) {
        return jdbcTemplate.query("""
                SELECT d.*, u.username, u.real_name, u.phone, u.status, dep.name AS department_name
                FROM doctors d
                JOIN users u ON u.id = d.user_id
                LEFT JOIN departments dep ON dep.id = d.department_id
                WHERE d.department_id = ?
                ORDER BY d.id ASC
                """, rowMapper, departmentId);
    }

    @Transactional
    public Doctor create(Doctor doctor) {
        validate(doctor);
        ensureDepartmentExists(doctor.getDepartmentId());
        ensureUsernameAvailable(doctor.getUsername(), null);

        Long userId = createUser(doctor.getUsername(), doctor.getRealName(), doctor.getPhone(), "DOCTOR");
        jdbcTemplate.update("""
                INSERT INTO doctors (user_id, department_id, title, specialty, introduction)
                VALUES (?, ?, ?, ?, ?)
                """,
                userId,
                doctor.getDepartmentId(),
                doctor.getTitle(),
                doctor.getSpecialty(),
                doctor.getIntroduction()
        );

        return detail(latestDoctorIdByUserId(userId));
    }

    @Transactional
    public Doctor update(Long id, Doctor doctor) {
        Doctor existing = detail(id);
        validate(doctor);
        ensureDepartmentExists(doctor.getDepartmentId());
        ensureUsernameAvailable(doctor.getUsername(), existing.getUserId());

        jdbcTemplate.update("""
                UPDATE users
                SET username = ?, real_name = ?, phone = ?, status = ?
                WHERE id = ?
                """,
                doctor.getUsername(),
                doctor.getRealName(),
                doctor.getPhone(),
                defaultStatus(doctor.getStatus()),
                existing.getUserId()
        );

        jdbcTemplate.update("""
                UPDATE doctors
                SET department_id = ?, title = ?, specialty = ?, introduction = ?
                WHERE id = ?
                """,
                doctor.getDepartmentId(),
                doctor.getTitle(),
                doctor.getSpecialty(),
                doctor.getIntroduction(),
                id
        );

        return detail(id);
    }

    @Transactional
    public void delete(Long id) {
        Doctor existing = detail(id);
        jdbcTemplate.update("DELETE FROM doctors WHERE id = ?", id);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", existing.getUserId());
    }

    private Long createUser(String username, String realName, String phone, String role) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO users (username, password, role, real_name, phone, status, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, NOW())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, "123456");
            ps.setString(3, role);
            ps.setString(4, realName);
            ps.setString(5, phone);
            ps.setString(6, "ENABLED");
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private Long latestDoctorIdByUserId(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM doctors WHERE user_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                userId
        );
    }

    private void ensureUsernameAvailable(String username, Long currentUserId) {
        Integer count;
        if (currentUserId == null) {
            count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, username);
        } else {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE username = ? AND id <> ?",
                    Integer.class,
                    username,
                    currentUserId
            );
        }
        if (count != null && count > 0) {
            throw new BusinessException("登录账号已存在");
        }
    }

    private void ensureDepartmentExists(Long departmentId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM departments WHERE id = ?", Integer.class, departmentId);
        if (count == null || count == 0) {
            throw new BusinessException("请选择有效科室");
        }
    }

    private void validate(Doctor doctor) {
        if (doctor == null || !StringUtils.hasText(doctor.getUsername())) {
            throw new BusinessException("登录账号不能为空");
        }
        if (!StringUtils.hasText(doctor.getRealName())) {
            throw new BusinessException("姓名不能为空");
        }
        if (doctor.getDepartmentId() == null) {
            throw new BusinessException("请选择科室");
        }
    }

    private String defaultStatus(String status) {
        return StringUtils.hasText(status) ? status : "ENABLED";
    }
}
