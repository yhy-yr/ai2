package com.localcare.healthcare.service;

import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.Prescription;
import com.localcare.healthcare.model.PrescriptionItem;
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
public class PrescriptionService {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Prescription> prescriptionRowMapper = (rs, rowNum) -> {
        Prescription prescription = new Prescription();
        prescription.setId(rs.getLong("id"));
        prescription.setMedicalRecordId(readNullableLong(rs, "medical_record_id"));
        prescription.setAppointmentId(rs.getLong("appointment_id"));
        prescription.setPatientId(rs.getLong("patient_id"));
        prescription.setDoctorId(rs.getLong("doctor_id"));
        prescription.setStatus(rs.getString("status"));
        prescription.setAdvice(rs.getString("advice"));
        prescription.setCreatedAt(toLocalDateTime(rs.getObject("created_at")));
        prescription.setPatientName(readString(rs, "patient_name"));
        prescription.setDoctorName(readString(rs, "doctor_name"));
        prescription.setDepartmentName(readString(rs, "department_name"));
        return prescription;
    };

    private final RowMapper<PrescriptionItem> itemRowMapper = (rs, rowNum) -> {
        PrescriptionItem item = new PrescriptionItem();
        item.setId(rs.getLong("id"));
        item.setPrescriptionId(rs.getLong("prescription_id"));
        item.setMedicineId(rs.getLong("medicine_id"));
        item.setMedicineName(rs.getString("medicine_name"));
        item.setDosage(rs.getString("dosage"));
        item.setFrequency(rs.getString("frequency"));
        item.setDays(rs.getInt("days"));
        item.setQuantity(rs.getInt("quantity"));
        item.setRemark(rs.getString("remark"));
        return item;
    };

    public PrescriptionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Prescription create(User user, Prescription prescription) {
        Long doctorId = requireDoctorId(user);
        if (prescription == null) {
            throw new BusinessException("处方信息不能为空");
        }
        if (prescription.getMedicalRecordId() == null && prescription.getAppointmentId() == null) {
            throw new BusinessException("medicalRecordId 和 appointmentId 至少填写一个");
        }
        if (prescription.getItems() == null || prescription.getItems().isEmpty()) {
            throw new BusinessException("处方明细不能为空");
        }

        Map<String, Object> relation = resolveRelation(prescription);
        Long relationDoctorId = numberToLong(relation.get("doctor_id"));
        if (!doctorId.equals(relationDoctorId)) {
            throw new BusinessException("只能给自己的病历或预约开处方");
        }

        validateItemsAndStock(prescription.getItems());

        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO prescriptions (
                        medical_record_id,
                        appointment_id,
                        patient_id,
                        doctor_id,
                        status,
                        advice,
                        created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            setNullableLong(ps, 1, numberToLong(relation.get("medical_record_id")));
            ps.setLong(2, numberToLong(relation.get("appointment_id")));
            ps.setLong(3, numberToLong(relation.get("patient_id")));
            ps.setLong(4, doctorId);
            ps.setString(5, "ISSUED");
            ps.setString(6, prescription.getAdvice());
            ps.setTimestamp(7, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Long prescriptionId = keyHolder.getKey().longValue();
        for (PrescriptionItem item : prescription.getItems()) {
            Map<String, Object> medicine = medicine(item.getMedicineId());
            String medicineName = String.valueOf(medicine.get("name"));
            Integer quantity = safePositive(item.getQuantity(), "药品数量必须大于 0");
            jdbcTemplate.update("""
                    INSERT INTO prescription_items (
                        prescription_id,
                        medicine_id,
                        medicine_name,
                        dosage,
                        frequency,
                        days,
                        quantity,
                        remark
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    prescriptionId,
                    item.getMedicineId(),
                    medicineName,
                    item.getDosage(),
                    item.getFrequency(),
                    item.getDays() == null ? 0 : item.getDays(),
                    quantity,
                    item.getRemark());
            jdbcTemplate.update("UPDATE medicines SET stock = stock - ? WHERE id = ?", quantity, item.getMedicineId());
        }

        return detailForService(prescriptionId);
    }

    public List<Prescription> doctorList(User user) {
        Long doctorId = requireDoctorId(user);
        return attachItems(jdbcTemplate.query(baseSql() + """
                WHERE p.doctor_id = ?
                ORDER BY p.created_at DESC, p.id DESC
                """, prescriptionRowMapper, doctorId));
    }

    public List<Prescription> patientList(User user) {
        Long patientId = requirePatientId(user);
        return attachItems(jdbcTemplate.query(baseSql() + """
                WHERE p.patient_id = ?
                ORDER BY p.created_at DESC, p.id DESC
                """, prescriptionRowMapper, patientId));
    }

    public List<Prescription> listAll(User user) {
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new BusinessException("仅管理员可以查看全部处方");
        }
        return attachItems(jdbcTemplate.query(baseSql() + """
                ORDER BY p.created_at DESC, p.id DESC
                """, prescriptionRowMapper));
    }

    public Prescription detail(User user, Long id) {
        Prescription prescription = detailForService(id);
        if ("ADMIN".equals(user.getRole())) {
            return prescription;
        }
        if ("DOCTOR".equals(user.getRole())) {
            Long doctorId = requireDoctorId(user);
            if (doctorId.equals(prescription.getDoctorId())) {
                return prescription;
            }
            throw new BusinessException("无权查看该处方");
        }
        if ("PATIENT".equals(user.getRole())) {
            Long patientId = requirePatientId(user);
            if (patientId.equals(prescription.getPatientId())) {
                return prescription;
            }
            throw new BusinessException("无权查看该处方");
        }
        throw new BusinessException("无权查看该处方");
    }

    private Map<String, Object> resolveRelation(Prescription prescription) {
        if (prescription.getMedicalRecordId() != null) {
            try {
                return jdbcTemplate.queryForMap("""
                        SELECT id AS medical_record_id, appointment_id, patient_id, doctor_id
                        FROM medical_records
                        WHERE id = ?
                        """, prescription.getMedicalRecordId());
            } catch (EmptyResultDataAccessException e) {
                throw new BusinessException("病历不存在");
            }
        }
        try {
            Map<String, Object> appointment = jdbcTemplate.queryForMap("""
                    SELECT NULL AS medical_record_id, id AS appointment_id, patient_id, doctor_id
                    FROM appointments
                    WHERE id = ?
                    """, prescription.getAppointmentId());
            return appointment;
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("预约不存在");
        }
    }

    private void validateItemsAndStock(List<PrescriptionItem> items) {
        for (PrescriptionItem item : items) {
            if (item.getMedicineId() == null) {
                throw new BusinessException("请选择药品");
            }
            Integer quantity = safePositive(item.getQuantity(), "药品数量必须大于 0");
            Integer days = item.getDays();
            if (days == null || days <= 0) {
                throw new BusinessException("用药天数必须大于 0");
            }
            if (!StringUtils.hasText(item.getDosage()) || !StringUtils.hasText(item.getFrequency())) {
                throw new BusinessException("请填写剂量和用药频次");
            }
            Map<String, Object> medicine = medicine(item.getMedicineId());
            int stock = ((Number) medicine.get("stock")).intValue();
            if (stock < quantity) {
                throw new BusinessException("药品库存不足：" + medicine.get("name"));
            }
        }
    }

    private Map<String, Object> medicine(Long medicineId) {
        try {
            return jdbcTemplate.queryForMap("SELECT id, name, stock FROM medicines WHERE id = ?", medicineId);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("药品不存在");
        }
    }

    private Prescription detailForService(Long id) {
        try {
            Prescription prescription = jdbcTemplate.queryForObject(baseSql() + "WHERE p.id = ?", prescriptionRowMapper, id);
            if (prescription != null) {
                prescription.setItems(items(id));
            }
            return prescription;
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("处方不存在");
        }
    }

    private List<Prescription> attachItems(List<Prescription> prescriptions) {
        for (Prescription prescription : prescriptions) {
            prescription.setItems(items(prescription.getId()));
        }
        return prescriptions;
    }

    private List<PrescriptionItem> items(Long prescriptionId) {
        return jdbcTemplate.query(
                "SELECT * FROM prescription_items WHERE prescription_id = ? ORDER BY id ASC",
                itemRowMapper,
                prescriptionId
        );
    }

    private String baseSql() {
        return """
                SELECT
                    p.*,
                    patient_user.real_name AS patient_name,
                    doctor_user.real_name AS doctor_name,
                    dep.name AS department_name
                FROM prescriptions p
                JOIN patients patient ON patient.id = p.patient_id
                JOIN users patient_user ON patient_user.id = patient.user_id
                JOIN doctors doctor ON doctor.id = p.doctor_id
                JOIN users doctor_user ON doctor_user.id = doctor.user_id
                JOIN departments dep ON dep.id = doctor.department_id
                """;
    }

    private Long requireDoctorId(User user) {
        if (user == null || !"DOCTOR".equals(user.getRole())) {
            throw new BusinessException("仅医生可以操作处方");
        }
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM doctors WHERE user_id = ?", Long.class, user.getId());
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("当前账号未绑定医生资料");
        }
    }

    private Long requirePatientId(User user) {
        if (user == null || !"PATIENT".equals(user.getRole())) {
            throw new BusinessException("仅患者可以查看自己的处方");
        }
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM patients WHERE user_id = ?", Long.class, user.getId());
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("当前账号未绑定患者资料");
        }
    }

    private int safePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new BusinessException(message);
        }
        return value;
    }

    private Long numberToLong(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value) throws java.sql.SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
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

    private Long readNullableLong(java.sql.ResultSet rs, String columnName) {
        try {
            long value = rs.getLong(columnName);
            return rs.wasNull() ? null : value;
        } catch (Exception e) {
            return null;
        }
    }
}
