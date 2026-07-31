package com.localcare.healthcare.service;

import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.Examination;
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
public class ExaminationService {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Examination> rowMapper = (rs, rowNum) -> {
        Examination examination = new Examination();
        examination.setId(rs.getLong("id"));
        examination.setAppointmentId(rs.getLong("appointment_id"));
        examination.setPatientId(rs.getLong("patient_id"));
        examination.setDoctorId(rs.getLong("doctor_id"));
        examination.setExamType(rs.getString("exam_type"));
        examination.setExamItem(rs.getString("exam_item"));
        examination.setResult(rs.getString("result"));
        examination.setConclusion(rs.getString("conclusion"));
        examination.setCreatedAt(toLocalDateTime(rs.getObject("created_at")));
        examination.setPatientName(readString(rs, "patient_name"));
        examination.setDoctorName(readString(rs, "doctor_name"));
        examination.setDepartmentName(readString(rs, "department_name"));
        return examination;
    };

    public ExaminationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Examination create(User user, Examination examination) {
        Long doctorId = requireDoctorId(user);
        validate(examination);
        Map<String, Object> appointment = requireAppointment(examination.getAppointmentId());
        Long appointmentDoctorId = numberToLong(appointment.get("doctor_id"));
        if (!doctorId.equals(appointmentDoctorId)) {
            throw new BusinessException("只能给自己的预约录入检查结果");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO examinations (
                        appointment_id,
                        patient_id,
                        doctor_id,
                        exam_type,
                        exam_item,
                        result,
                        conclusion,
                        created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, examination.getAppointmentId());
            ps.setLong(2, numberToLong(appointment.get("patient_id")));
            ps.setLong(3, doctorId);
            ps.setString(4, examination.getExamType());
            ps.setString(5, examination.getExamItem());
            ps.setString(6, examination.getResult());
            ps.setString(7, examination.getConclusion());
            ps.setTimestamp(8, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        return detailForService(keyHolder.getKey().longValue());
    }

    @Transactional
    public Examination update(User user, Long id, Examination examination) {
        Long doctorId = requireDoctorId(user);
        Examination existing = detailForService(id);
        if (!doctorId.equals(existing.getDoctorId())) {
            throw new BusinessException("只能修改自己录入的检查结果");
        }
        validateForUpdate(examination);
        jdbcTemplate.update("""
                UPDATE examinations
                SET exam_type = ?, exam_item = ?, result = ?, conclusion = ?
                WHERE id = ?
                """,
                examination.getExamType(),
                examination.getExamItem(),
                examination.getResult(),
                examination.getConclusion(),
                id);
        return detailForService(id);
    }

    public List<Examination> doctorList(User user) {
        Long doctorId = requireDoctorId(user);
        return jdbcTemplate.query(baseSql() + """
                WHERE e.doctor_id = ?
                ORDER BY e.created_at DESC, e.id DESC
                """, rowMapper, doctorId);
    }

    public List<Examination> patientList(User user) {
        Long patientId = requirePatientId(user);
        return jdbcTemplate.query(baseSql() + """
                WHERE e.patient_id = ?
                ORDER BY e.created_at DESC, e.id DESC
                """, rowMapper, patientId);
    }

    public List<Examination> listAll(User user) {
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new BusinessException("仅管理员可以查看全部检查结果");
        }
        return jdbcTemplate.query(baseSql() + """
                ORDER BY e.created_at DESC, e.id DESC
                """, rowMapper);
    }

    private Examination detailForService(Long id) {
        try {
            return jdbcTemplate.queryForObject(baseSql() + "WHERE e.id = ?", rowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("检查结果不存在");
        }
    }

    private String baseSql() {
        return """
                SELECT
                    e.*,
                    patient_user.real_name AS patient_name,
                    doctor_user.real_name AS doctor_name,
                    dep.name AS department_name
                FROM examinations e
                JOIN patients patient ON patient.id = e.patient_id
                JOIN users patient_user ON patient_user.id = patient.user_id
                JOIN doctors doctor ON doctor.id = e.doctor_id
                JOIN users doctor_user ON doctor_user.id = doctor.user_id
                JOIN departments dep ON dep.id = doctor.department_id
                """;
    }

    private Map<String, Object> requireAppointment(Long appointmentId) {
        try {
            return jdbcTemplate.queryForMap("SELECT * FROM appointments WHERE id = ?", appointmentId);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("预约不存在");
        }
    }

    private void validate(Examination examination) {
        if (examination == null || examination.getAppointmentId() == null) {
            throw new BusinessException("appointmentId 不能为空");
        }
        validateForUpdate(examination);
    }

    private void validateForUpdate(Examination examination) {
        if (examination == null) {
            throw new BusinessException("检查结果不能为空");
        }
        if (!StringUtils.hasText(examination.getExamType())) {
            throw new BusinessException("检查类型不能为空");
        }
        if (!StringUtils.hasText(examination.getExamItem())) {
            throw new BusinessException("检查项目不能为空");
        }
        if (!StringUtils.hasText(examination.getResult())) {
            throw new BusinessException("检查结果不能为空");
        }
        if (!StringUtils.hasText(examination.getConclusion())) {
            throw new BusinessException("检查结论不能为空");
        }
    }

    private Long requireDoctorId(User user) {
        if (user == null || !"DOCTOR".equals(user.getRole())) {
            throw new BusinessException("仅医生可以操作检查结果");
        }
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM doctors WHERE user_id = ?", Long.class, user.getId());
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("当前账号未绑定医生资料");
        }
    }

    private Long requirePatientId(User user) {
        if (user == null || !"PATIENT".equals(user.getRole())) {
            throw new BusinessException("仅患者可以查看自己的检查结果");
        }
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM patients WHERE user_id = ?", Long.class, user.getId());
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("当前账号未绑定患者资料");
        }
    }

    private Long numberToLong(Object value) {
        return ((Number) value).longValue();
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
}

