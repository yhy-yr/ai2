package com.localcare.healthcare.service;

import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DoctorVisitCompletenessService {

    private final JdbcTemplate jdbcTemplate;

    public DoctorVisitCompletenessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> getCompleteness(User user, Long appointmentId) {
        Long doctorId = requireDoctorId(user);
        Map<String, Object> appointment = requireAppointment(doctorId, appointmentId);
        Long patientId = numberToLong(appointment.get("patient_id"));

        List<Map<String, Object>> nodes = List.of(
                node("AI 问诊记录", exists("SELECT COUNT(*) FROM ai_consultations WHERE patient_id = ?", patientId)),
                node("电子病历", exists("SELECT COUNT(*) FROM medical_records WHERE appointment_id = ?", appointmentId)),
                node("检查结果", exists("SELECT COUNT(*) FROM examinations WHERE appointment_id = ?", appointmentId)),
                node("处方", exists("SELECT COUNT(*) FROM prescriptions WHERE appointment_id = ?", appointmentId))
        );

        List<String> completedItems = new ArrayList<>();
        List<String> pendingItems = new ArrayList<>();
        for (Map<String, Object> node : nodes) {
            String name = String.valueOf(node.get("name"));
            if (Boolean.TRUE.equals(node.get("done"))) {
                completedItems.add(name);
            } else {
                pendingItems.add(name);
            }
        }

        int percent = completedItems.size() * 100 / nodes.size();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appointmentId", appointmentId);
        result.put("percent", percent);
        result.put("nodes", nodes);
        result.put("completedItems", completedItems);
        result.put("pendingItems", pendingItems);
        result.put("closed", pendingItems.isEmpty());
        result.put("message", pendingItems.isEmpty() ? "本次接诊流程已闭环，可通知患者查看结果。" : "本次接诊仍有环节待完成。");
        return result;
    }

    private Map<String, Object> node(String name, boolean done) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("name", name);
        node.put("done", done);
        return node;
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private Map<String, Object> requireAppointment(Long doctorId, Long appointmentId) {
        if (appointmentId == null) {
            throw new BusinessException("appointmentId 不能为空");
        }
        try {
            return jdbcTemplate.queryForMap(
                    "SELECT * FROM appointments WHERE id = ? AND doctor_id = ?",
                    appointmentId,
                    doctorId
            );
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("预约不存在或不属于当前医生");
        }
    }

    private Long requireDoctorId(User user) {
        if (user == null || !"DOCTOR".equals(user.getRole())) {
            throw new BusinessException("仅医生可以查看接诊闭环完整度");
        }

        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM doctors WHERE user_id = ?",
                    Long.class,
                    user.getId()
            );
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("当前账号未绑定医生资料");
        }
    }

    private Long numberToLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }
}
