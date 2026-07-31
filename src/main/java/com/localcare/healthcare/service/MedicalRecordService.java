package com.localcare.healthcare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.MedicalRecord;
import com.localcare.healthcare.model.User;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class MedicalRecordService {

    private static final String DISCLAIMER = "AI 结果仅供辅助参考，不能替代医生诊断。";
    private static final String AI_DRAFT_NOTE = "该内容由本地规则根据预约症状自动生成，仅供医生书写病历时参考。";
    private static final String DEEPSEEK_DRAFT_NOTE = "该内容由 AI 根据患者症状描述自动生成，仅供医生书写病历时参考，需医生确认后生效。";
    private static final String DEEPSEEK_SYSTEM_PROMPT = """
            你是一个基层诊所智慧医疗系统中的 AI 病历草稿助手。
            你的任务是根据患者预约时填写的症状描述，帮助医生整理电子病历草稿。
            你只能生成主诉和现病史草稿。
            你不能生成最终诊断。
            你不能推荐具体药品。
            你不能替代医生判断。
            内容应简洁、正式、符合电子病历书写风格。
            请只返回 JSON，不要返回 Markdown，不要返回解释文字。

            返回 JSON 格式必须是：

            {
              "chiefComplaint": "...",
              "presentIllness": "...",
              "aiDraft": "该内容由 AI 根据患者症状描述自动生成，仅供医生书写病历时参考，需医生确认后生效。",
              "disclaimer": "AI 结果仅供辅助参考，不能替代医生诊断。"
            }
            """;

    private final JdbcTemplate jdbcTemplate;
    private final DeepSeekService deepSeekService;

    private final RowMapper<MedicalRecord> rowMapper = (rs, rowNum) -> {
        MedicalRecord record = new MedicalRecord();
        record.setId(rs.getLong("id"));
        record.setAppointmentId(rs.getLong("appointment_id"));
        record.setPatientId(rs.getLong("patient_id"));
        record.setDoctorId(rs.getLong("doctor_id"));
        record.setChiefComplaint(rs.getString("chief_complaint"));
        record.setPresentIllness(rs.getString("present_illness"));
        record.setDiagnosis(rs.getString("diagnosis"));
        record.setTreatmentPlan(rs.getString("treatment_plan"));
        record.setAiDraft(rs.getString("ai_draft"));
        record.setCreatedAt(toLocalDateTime(rs.getObject("created_at")));
        record.setUpdatedAt(toLocalDateTime(rs.getObject("updated_at")));
        record.setPatientName(readString(rs, "patient_name"));
        record.setDoctorName(readString(rs, "doctor_name"));
        record.setDepartmentName(readString(rs, "department_name"));
        record.setDisclaimer(DISCLAIMER);
        return record;
    };

    public MedicalRecordService(JdbcTemplate jdbcTemplate, DeepSeekService deepSeekService) {
        this.jdbcTemplate = jdbcTemplate;
        this.deepSeekService = deepSeekService;
    }

    public MedicalRecord aiDraft(User user, Long appointmentId) {
        Long doctorId = requireDoctorId(user);
        Map<String, Object> appointment = requireAppointment(appointmentId);
        Long appointmentDoctorId = numberToLong(appointment.get("doctor_id"));
        if (!doctorId.equals(appointmentDoctorId)) {
            throw new BusinessException("只能为自己的预约生成病历草稿");
        }

        MedicalRecord draft = generateDraftByDeepSeek(appointmentId, doctorId, appointment);
        if (draft == null) {
            draft = generateDraftByLocalRules(appointmentId, doctorId, appointment);
        }
        return draft;
    }

    private MedicalRecord generateDraftByDeepSeek(Long appointmentId, Long doctorId, Map<String, Object> appointment) {
        if (!deepSeekService.isAvailable()) {
            return null;
        }

        try {
            String symptoms = stringValue(appointment.get("symptom_description"));
            String userPrompt = "患者预约症状描述：\n" + symptoms + "\n\n请生成电子病历草稿 JSON。";
            return deepSeekService.chat(DEEPSEEK_SYSTEM_PROMPT, userPrompt)
                    .flatMap(deepSeekService::parseJsonObject)
                    .map(json -> buildDeepSeekDraft(appointmentId, doctorId, appointment, json))
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private MedicalRecord buildDeepSeekDraft(
            Long appointmentId,
            Long doctorId,
            Map<String, Object> appointment,
            JsonNode json
    ) {
        MedicalRecord draft = baseDraft(appointmentId, doctorId, appointment);
        draft.setChiefComplaint(requiredText(json, "chiefComplaint"));
        draft.setPresentIllness(requiredText(json, "presentIllness"));
        String aiDraft = text(json, "aiDraft");
        draft.setAiDraft(StringUtils.hasText(aiDraft) ? aiDraft : DEEPSEEK_DRAFT_NOTE);
        String disclaimer = text(json, "disclaimer");
        draft.setDisclaimer(StringUtils.hasText(disclaimer) ? disclaimer : DISCLAIMER);
        draft.setSource("DEEPSEEK");
        return draft;
    }

    private MedicalRecord generateDraftByLocalRules(Long appointmentId, Long doctorId, Map<String, Object> appointment) {
        String symptoms = stringValue(appointment.get("symptom_description"));
        MedicalRecord draft = baseDraft(appointmentId, doctorId, appointment);
        draft.setChiefComplaint(normalizeSentence(symptoms));
        draft.setPresentIllness("患者自述出现" + stripEndPunctuation(symptoms) + "等不适，具体诊断需医生结合查体和检查结果判断。");
        draft.setAiDraft(AI_DRAFT_NOTE);
        draft.setDisclaimer(DISCLAIMER);
        draft.setSource("LOCAL_RULE");
        return draft;
    }

    private MedicalRecord baseDraft(Long appointmentId, Long doctorId, Map<String, Object> appointment) {
        MedicalRecord draft = new MedicalRecord();
        draft.setAppointmentId(appointmentId);
        draft.setPatientId(numberToLong(appointment.get("patient_id")));
        draft.setDoctorId(doctorId);
        return draft;
    }

    @Transactional
    public MedicalRecord save(User user, MedicalRecord record) {
        Long doctorId = requireDoctorId(user);
        if (record == null || record.getAppointmentId() == null) {
            throw new BusinessException("appointmentId 不能为空");
        }
        validateRecord(record);

        Map<String, Object> appointment = requireAppointment(record.getAppointmentId());
        Long appointmentDoctorId = numberToLong(appointment.get("doctor_id"));
        if (!doctorId.equals(appointmentDoctorId)) {
            throw new BusinessException("只能保存自己预约的病历");
        }
        if ("CANCELLED".equals(stringValue(appointment.get("status")))) {
            throw new BusinessException("已取消的预约不能保存病历");
        }

        Long patientId = numberToLong(appointment.get("patient_id"));
        Long existingId = findRecordIdByAppointment(record.getAppointmentId());
        LocalDateTime now = LocalDateTime.now();

        if (existingId == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO medical_records (
                            appointment_id,
                            patient_id,
                            doctor_id,
                            chief_complaint,
                            present_illness,
                            diagnosis,
                            treatment_plan,
                            ai_draft,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, record.getAppointmentId());
                ps.setLong(2, patientId);
                ps.setLong(3, doctorId);
                ps.setString(4, record.getChiefComplaint());
                ps.setString(5, record.getPresentIllness());
                ps.setString(6, record.getDiagnosis());
                ps.setString(7, record.getTreatmentPlan());
                ps.setString(8, record.getAiDraft());
                ps.setTimestamp(9, Timestamp.valueOf(now));
                ps.setTimestamp(10, Timestamp.valueOf(now));
                return ps;
            }, keyHolder);
            existingId = keyHolder.getKey().longValue();
        } else {
            jdbcTemplate.update("""
                    UPDATE medical_records
                    SET chief_complaint = ?,
                        present_illness = ?,
                        diagnosis = ?,
                        treatment_plan = ?,
                        ai_draft = ?,
                        updated_at = ?
                    WHERE id = ?
                    """,
                    record.getChiefComplaint(),
                    record.getPresentIllness(),
                    record.getDiagnosis(),
                    record.getTreatmentPlan(),
                    record.getAiDraft(),
                    Timestamp.valueOf(now),
                    existingId);
        }

        if ("PENDING".equals(stringValue(appointment.get("status")))) {
            jdbcTemplate.update("UPDATE appointments SET status = 'IN_PROGRESS' WHERE id = ?", record.getAppointmentId());
        }
        return detailForService(existingId);
    }

    @Transactional
    public MedicalRecord update(User user, Long id, MedicalRecord record) {
        Long doctorId = requireDoctorId(user);
        MedicalRecord existing = detailForService(id);
        if (!doctorId.equals(existing.getDoctorId())) {
            throw new BusinessException("只能修改自己写的病历");
        }
        validateRecord(record);

        jdbcTemplate.update("""
                UPDATE medical_records
                SET chief_complaint = ?,
                    present_illness = ?,
                    diagnosis = ?,
                    treatment_plan = ?,
                    updated_at = ?
                WHERE id = ?
                """,
                record.getChiefComplaint(),
                record.getPresentIllness(),
                record.getDiagnosis(),
                record.getTreatmentPlan(),
                Timestamp.valueOf(LocalDateTime.now()),
                id);

        return detailForService(id);
    }

    public List<MedicalRecord> doctorList(User user) {
        Long doctorId = requireDoctorId(user);
        return jdbcTemplate.query(baseSql() + """
                WHERE mr.doctor_id = ?
                ORDER BY mr.created_at DESC, mr.id DESC
                """, rowMapper, doctorId);
    }

    public List<MedicalRecord> patientList(User user) {
        Long patientId = requirePatientId(user);
        return jdbcTemplate.query(baseSql() + """
                WHERE mr.patient_id = ?
                ORDER BY mr.created_at DESC, mr.id DESC
                """, rowMapper, patientId);
    }

    public List<MedicalRecord> listAll(User user) {
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new BusinessException("仅管理员可以查看全部病历");
        }
        return jdbcTemplate.query(baseSql() + """
                ORDER BY mr.created_at DESC, mr.id DESC
                """, rowMapper);
    }

    public MedicalRecord detail(User user, Long id) {
        MedicalRecord record = detailForService(id);
        if ("ADMIN".equals(user.getRole())) {
            return record;
        }
        if ("DOCTOR".equals(user.getRole())) {
            Long doctorId = requireDoctorId(user);
            if (doctorId.equals(record.getDoctorId())) {
                return record;
            }
            throw new BusinessException("无权查看该病历");
        }
        if ("PATIENT".equals(user.getRole())) {
            Long patientId = requirePatientId(user);
            if (patientId.equals(record.getPatientId())) {
                return record;
            }
            throw new BusinessException("无权查看该病历");
        }
        throw new BusinessException("无权查看该病历");
    }

    private MedicalRecord detailForService(Long id) {
        try {
            return jdbcTemplate.queryForObject(baseSql() + "WHERE mr.id = ?", rowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("病历不存在");
        }
    }

    private String baseSql() {
        return """
                SELECT
                    mr.*,
                    patient_user.real_name AS patient_name,
                    doctor_user.real_name AS doctor_name,
                    dep.name AS department_name
                FROM medical_records mr
                JOIN patients p ON p.id = mr.patient_id
                JOIN users patient_user ON patient_user.id = p.user_id
                JOIN doctors d ON d.id = mr.doctor_id
                JOIN users doctor_user ON doctor_user.id = d.user_id
                JOIN departments dep ON dep.id = d.department_id
                """;
    }

    private Map<String, Object> requireAppointment(Long appointmentId) {
        try {
            return jdbcTemplate.queryForMap("SELECT * FROM appointments WHERE id = ?", appointmentId);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("预约不存在");
        }
    }

    private Long findRecordIdByAppointment(Long appointmentId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM medical_records WHERE appointment_id = ? LIMIT 1",
                    Long.class,
                    appointmentId
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Long requireDoctorId(User user) {
        if (user == null || !"DOCTOR".equals(user.getRole())) {
            throw new BusinessException("仅医生可以操作病历");
        }
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM doctors WHERE user_id = ?", Long.class, user.getId());
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("当前账号未绑定医生资料");
        }
    }

    private Long requirePatientId(User user) {
        if (user == null || !"PATIENT".equals(user.getRole())) {
            throw new BusinessException("仅患者可以查看自己的病历");
        }
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM patients WHERE user_id = ?", Long.class, user.getId());
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("当前账号未绑定患者资料");
        }
    }

    private void validateRecord(MedicalRecord record) {
        if (record == null) {
            throw new BusinessException("病历信息不能为空");
        }
        if (!StringUtils.hasText(record.getChiefComplaint())) {
            throw new BusinessException("主诉不能为空");
        }
        if (!StringUtils.hasText(record.getPresentIllness())) {
            throw new BusinessException("现病史不能为空");
        }
        if (!StringUtils.hasText(record.getDiagnosis())) {
            throw new BusinessException("医生诊断不能为空");
        }
        if (!StringUtils.hasText(record.getTreatmentPlan())) {
            throw new BusinessException("治疗建议不能为空");
        }
    }

    private String normalizeSentence(String text) {
        String value = stripEndPunctuation(text);
        return value + "。";
    }

    private String stripEndPunctuation(String text) {
        String value = StringUtils.hasText(text) ? text.trim() : "相关症状";
        return value.replaceAll("[。.!！]+$", "");
    }

    private Long numberToLong(Object value) {
        return ((Number) value).longValue();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }

    private String readString(java.sql.ResultSet rs, String columnName) {
        try {
            return rs.getString(columnName);
        } catch (Exception e) {
            return null;
        }
    }

    private String requiredText(JsonNode json, String fieldName) {
        String value = text(json, fieldName);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("DeepSeek 返回缺少字段：" + fieldName);
        }
        return value;
    }

    private String text(JsonNode json, String fieldName) {
        JsonNode value = json.path(fieldName);
        return value.isTextual() ? value.asText().trim() : "";
    }
}
