package com.localcare.healthcare.service;

import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.Appointment;
import com.localcare.healthcare.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Appointment> rowMapper = (rs, rowNum) -> {
        Appointment appointment = new Appointment();
        appointment.setId(rs.getLong("id"));
        appointment.setPatientId(rs.getLong("patient_id"));
        appointment.setDoctorId(rs.getLong("doctor_id"));
        appointment.setDepartmentId(rs.getLong("department_id"));
        appointment.setAppointmentDate(toLocalDate(rs.getObject("appointment_date")));
        appointment.setTimeSlot(rs.getString("time_slot"));
        appointment.setSymptomDescription(rs.getString("symptom_description"));
        appointment.setStatus(rs.getString("status"));
        appointment.setCreatedAt(toLocalDateTime(rs.getObject("created_at")));
        appointment.setPatientName(readString(rs, "patient_name"));
        appointment.setGender(readString(rs, "gender"));
        appointment.setAge(readInteger(rs, "age"));
        appointment.setPhone(readString(rs, "phone"));
        appointment.setDoctorName(readString(rs, "doctor_name"));
        appointment.setDepartmentName(readString(rs, "department_name"));
        return appointment;
    };

    public AppointmentService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Appointment create(User user, Appointment appointment) {
        Long patientId = requirePatientId(user);
        validateCreate(appointment);
        ensureDoctorDepartmentMatched(appointment.getDoctorId(), appointment.getDepartmentId());
        ensureTimeAvailable(appointment.getDoctorId(), appointment.getAppointmentDate(), appointment.getTimeSlot());

        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO appointments (
                        patient_id,
                        doctor_id,
                        department_id,
                        appointment_date,
                        time_slot,
                        symptom_description,
                        status,
                        created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, patientId);
            ps.setLong(2, appointment.getDoctorId());
            ps.setLong(3, appointment.getDepartmentId());
            ps.setDate(4, Date.valueOf(appointment.getAppointmentDate()));
            ps.setString(5, appointment.getTimeSlot());
            ps.setString(6, appointment.getSymptomDescription());
            ps.setString(7, "PENDING");
            ps.setTimestamp(8, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        return detail(keyHolder.getKey().longValue());
    }

    public List<Appointment> my(User user) {
        Long patientId = requirePatientId(user);
        return jdbcTemplate.query(baseListSql() + """
                WHERE a.patient_id = ?
                ORDER BY a.appointment_date DESC, a.created_at DESC, a.id DESC
                """, rowMapper, patientId);
    }

    public List<Appointment> listAll(User user) {
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new BusinessException("仅管理员可以查看全部预约");
        }

        return jdbcTemplate.query(baseListSql() + """
                ORDER BY a.appointment_date DESC, a.created_at DESC, a.id DESC
                """, rowMapper);
    }

    public List<Appointment> doctorList(User user) {
        Long doctorId = requireDoctorId(user);
        return jdbcTemplate.query(baseListSql() + """
                WHERE a.doctor_id = ?
                ORDER BY a.appointment_date DESC, a.created_at DESC, a.id DESC
                """, rowMapper, doctorId);
    }

    public Appointment updateStatus(User user, Long appointmentId, String status) {
        validateStatus(status);
        Appointment appointment = detail(appointmentId);
        if ("DOCTOR".equals(user.getRole())) {
            Long doctorId = requireDoctorId(user);
            if (!doctorId.equals(appointment.getDoctorId())) {
                throw new BusinessException("只能修改自己的预约状态");
            }
        } else if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessException("无权修改预约状态");
        }

        jdbcTemplate.update(
                "UPDATE appointments SET status = ? WHERE id = ?",
                status,
                appointmentId
        );
        return detail(appointmentId);
    }

    public void cancel(User user, Long appointmentId) {
        Long patientId = requirePatientId(user);
        Appointment appointment = detail(appointmentId);
        if (!patientId.equals(appointment.getPatientId())) {
            throw new BusinessException("只能取消自己的预约");
        }
        if ("COMPLETED".equals(appointment.getStatus())) {
            throw new BusinessException("已完成的预约不能取消");
        }
        if ("CANCELLED".equals(appointment.getStatus())) {
            return;
        }

        jdbcTemplate.update(
                "UPDATE appointments SET status = 'CANCELLED' WHERE id = ?",
                appointmentId
        );
    }

    public int countMyAppointments(User user) {
        Long patientId = requirePatientId(user);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointments WHERE patient_id = ?",
                Integer.class,
                patientId
        );
        return count == null ? 0 : count;
    }

    private Appointment detail(Long id) {
        try {
            return jdbcTemplate.queryForObject(baseListSql() + "WHERE a.id = ?", rowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("预约不存在");
        }
    }

    private String baseListSql() {
        return """
                SELECT
                    a.*,
                    patient_user.real_name AS patient_name,
                    p.gender,
                    p.age,
                    patient_user.phone,
                    doctor_user.real_name AS doctor_name,
                    dep.name AS department_name
                FROM appointments a
                JOIN patients p ON p.id = a.patient_id
                JOIN users patient_user ON patient_user.id = p.user_id
                JOIN doctors d ON d.id = a.doctor_id
                JOIN users doctor_user ON doctor_user.id = d.user_id
                JOIN departments dep ON dep.id = a.department_id
                """;
    }

    private void validateCreate(Appointment appointment) {
        if (appointment == null) {
            throw new BusinessException("预约信息不能为空");
        }
        if (appointment.getDepartmentId() == null) {
            throw new BusinessException("请选择科室");
        }
        if (appointment.getDoctorId() == null) {
            throw new BusinessException("请选择医生");
        }
        if (appointment.getAppointmentDate() == null) {
            throw new BusinessException("请选择预约日期");
        }
        if (appointment.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new BusinessException("不能预约过去日期");
        }
        if (!StringUtils.hasText(appointment.getTimeSlot())) {
            throw new BusinessException("请选择预约时间段");
        }
        if (!StringUtils.hasText(appointment.getSymptomDescription())) {
            throw new BusinessException("请填写症状描述");
        }
    }

    private void ensureDoctorDepartmentMatched(Long doctorId, Long departmentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM doctors WHERE id = ? AND department_id = ?",
                Integer.class,
                doctorId,
                departmentId
        );
        if (count == null || count == 0) {
            throw new BusinessException("医生和科室不匹配");
        }
    }

    private void ensureTimeAvailable(Long doctorId, LocalDate appointmentDate, String timeSlot) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM appointments
                WHERE doctor_id = ?
                  AND appointment_date = ?
                  AND time_slot = ?
                  AND status <> 'CANCELLED'
                """, Integer.class, doctorId, Date.valueOf(appointmentDate), timeSlot);
        if (count != null && count > 0) {
            throw new BusinessException("该医生在当前日期和时间段已有预约");
        }
    }

    private void validateStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new BusinessException("预约状态不能为空");
        }
        if (!List.of("PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED").contains(status)) {
            throw new BusinessException("预约状态不正确");
        }
    }

    private Long requireDoctorId(User user) {
        if (user == null || !"DOCTOR".equals(user.getRole())) {
            throw new BusinessException("仅医生可以查看自己的预约");
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

    private Long requirePatientId(User user) {
        if (user == null || !"PATIENT".equals(user.getRole())) {
            throw new BusinessException("只有患者可以预约挂号");
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

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return null;
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

    private Integer readInteger(java.sql.ResultSet rs, String columnName) {
        try {
            Object value = rs.getObject(columnName);
            return value == null ? null : ((Number) value).intValue();
        } catch (Exception e) {
            return null;
        }
    }
}
