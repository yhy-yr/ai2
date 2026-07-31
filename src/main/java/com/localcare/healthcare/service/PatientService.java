package com.localcare.healthcare.service;

import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.Patient;
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
public class PatientService {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Patient> rowMapper = (rs, rowNum) -> {
        Patient patient = new Patient();
        patient.setId(rs.getLong("id"));
        patient.setUserId(rs.getLong("user_id"));
        patient.setUsername(rs.getString("username"));
        patient.setRealName(rs.getString("real_name"));
        patient.setPhone(rs.getString("phone"));
        patient.setStatus(rs.getString("status"));
        patient.setGender(rs.getString("gender"));
        patient.setAge(rs.getInt("age"));
        patient.setAddress(rs.getString("address"));
        patient.setAllergyHistory(rs.getString("allergy_history"));
        patient.setMedicalHistory(rs.getString("medical_history"));
        return patient;
    };

    public PatientService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Patient> list(String keyword) {
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            return jdbcTemplate.query("""
                    SELECT p.*, u.username, u.real_name, u.phone, u.status
                    FROM patients p
                    JOIN users u ON u.id = p.user_id
                    WHERE u.username LIKE ? OR u.real_name LIKE ? OR u.phone LIKE ? OR p.address LIKE ?
                    ORDER BY p.id ASC
                    """, rowMapper, like, like, like, like);
        }

        return jdbcTemplate.query("""
                SELECT p.*, u.username, u.real_name, u.phone, u.status
                FROM patients p
                JOIN users u ON u.id = p.user_id
                ORDER BY p.id ASC
                """, rowMapper);
    }

    public Patient detail(Long id) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT p.*, u.username, u.real_name, u.phone, u.status
                    FROM patients p
                    JOIN users u ON u.id = p.user_id
                    WHERE p.id = ?
                    """, rowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("患者不存在");
        }
    }

    @Transactional
    public Patient create(Patient patient) {
        validate(patient);
        ensureUsernameAvailable(patient.getUsername(), null);

        Long userId = createUser(patient.getUsername(), patient.getRealName(), patient.getPhone(), "PATIENT");
        jdbcTemplate.update("""
                INSERT INTO patients (user_id, gender, age, address, allergy_history, medical_history)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                userId,
                patient.getGender(),
                patient.getAge(),
                patient.getAddress(),
                patient.getAllergyHistory(),
                patient.getMedicalHistory()
        );

        return detail(latestPatientIdByUserId(userId));
    }

    @Transactional
    public Patient update(Long id, Patient patient) {
        Patient existing = detail(id);
        validate(patient);
        ensureUsernameAvailable(patient.getUsername(), existing.getUserId());

        jdbcTemplate.update("""
                UPDATE users
                SET username = ?, real_name = ?, phone = ?, status = ?
                WHERE id = ?
                """,
                patient.getUsername(),
                patient.getRealName(),
                patient.getPhone(),
                defaultStatus(patient.getStatus()),
                existing.getUserId()
        );

        jdbcTemplate.update("""
                UPDATE patients
                SET gender = ?, age = ?, address = ?, allergy_history = ?, medical_history = ?
                WHERE id = ?
                """,
                patient.getGender(),
                patient.getAge(),
                patient.getAddress(),
                patient.getAllergyHistory(),
                patient.getMedicalHistory(),
                id
        );

        return detail(id);
    }

    @Transactional
    public void delete(Long id) {
        Patient existing = detail(id);
        jdbcTemplate.update("DELETE FROM patients WHERE id = ?", id);
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

    private Long latestPatientIdByUserId(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM patients WHERE user_id = ? ORDER BY id DESC LIMIT 1",
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

    private void validate(Patient patient) {
        if (patient == null || !StringUtils.hasText(patient.getUsername())) {
            throw new BusinessException("登录账号不能为空");
        }
        if (!StringUtils.hasText(patient.getRealName())) {
            throw new BusinessException("姓名不能为空");
        }
        if (patient.getAge() == null) {
            patient.setAge(0);
        }
        if (patient.getAge() < 0 || patient.getAge() > 130) {
            throw new BusinessException("年龄范围不正确");
        }
    }

    private String defaultStatus(String status) {
        return StringUtils.hasText(status) ? status : "ENABLED";
    }
}

