package com.localcare.healthcare.service;

import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PatientVisitProgressService {

    private final JdbcTemplate jdbcTemplate;

    public PatientVisitProgressService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> getProgress(User user, Long appointmentId) {
        Long patientId = requirePatientId(user);
        Map<String, Object> appointment = requireAppointment(patientId, appointmentId);
        String status = stringValue(appointment.get("status"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appointmentId", appointmentId);
        result.put("progress", List.of(
                step("预约已提交", true, appointment.get("created_at")),
                step("AI 问诊已完成", exists("SELECT COUNT(*) FROM ai_consultations WHERE patient_id = ?", patientId),
                        firstTime("SELECT MAX(created_at) FROM ai_consultations WHERE patient_id = ?", patientId)),
                step("医生接诊中", List.of("IN_PROGRESS", "COMPLETED").contains(status), null),
                step("电子病历已生成", exists("SELECT COUNT(*) FROM medical_records WHERE appointment_id = ?", appointmentId),
                        firstTime("SELECT MIN(created_at) FROM medical_records WHERE appointment_id = ?", appointmentId)),
                step("检查结果已上传", exists("SELECT COUNT(*) FROM examinations WHERE appointment_id = ?", appointmentId),
                        firstTime("SELECT MIN(created_at) FROM examinations WHERE appointment_id = ?", appointmentId)),
                step("处方已开具", exists("SELECT COUNT(*) FROM prescriptions WHERE appointment_id = ?", appointmentId),
                        firstTime("SELECT MIN(created_at) FROM prescriptions WHERE appointment_id = ?", appointmentId))
        ));
        return result;
    }

    private Map<String, Object> step(String step, boolean done, Object time) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("step", step);
        item.put("done", done);
        item.put("time", time);
        return item;
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private LocalDateTime firstTime(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, LocalDateTime.class, args);
    }

    private Map<String, Object> requireAppointment(Long patientId, Long appointmentId) {
        if (appointmentId == null) {
            throw new BusinessException("appointmentId 不能为空");
        }
        try {
            return jdbcTemplate.queryForMap(
                    "SELECT * FROM appointments WHERE id = ? AND patient_id = ?",
                    appointmentId,
                    patientId
            );
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("预约不存在或不属于当前患者");
        }
    }

    private Long requirePatientId(User user) {
        if (user == null || !"PATIENT".equals(user.getRole())) {
            throw new BusinessException("仅患者可以查看就诊进度");
        }

        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM patients WHERE user_id = ?",
                    Long.class,
                    user.getId()
            );
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("当前账号未绑定患者资料");
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
