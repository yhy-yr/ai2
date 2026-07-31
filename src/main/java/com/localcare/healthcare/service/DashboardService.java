package com.localcare.healthcare.service;

import com.localcare.healthcare.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> adminStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        int todayAppointmentCount = countBySql(
                "SELECT COUNT(*) FROM appointments WHERE appointment_date = CURRENT_DATE()"
        );
        int completedTodayCount = countBySql(
                "SELECT COUNT(*) FROM appointments WHERE appointment_date = CURRENT_DATE() AND status = 'COMPLETED'"
        );
        stats.put("patientCount", count("patients"));
        stats.put("doctorCount", count("doctors"));
        stats.put("departmentCount", count("departments"));
        stats.put("medicineCount", count("medicines"));
        stats.put("appointmentCount", count("appointments"));
        stats.put("medicalRecordCount", count("medical_records"));
        stats.put("prescriptionCount", count("prescriptions"));
        stats.put("examinationCount", count("examinations"));
        stats.put("todayAppointmentCount", todayAppointmentCount);
        stats.put("inProgressAppointmentCount", countBySql(
                "SELECT COUNT(*) FROM appointments WHERE status = 'IN_PROGRESS'"
        ));
        stats.put("actionableAppointmentCount", countBySql(
                "SELECT COUNT(*) FROM appointments WHERE status IN ('PENDING', 'IN_PROGRESS')"
        ));
        stats.put("lowStockMedicineCount", countBySql(
                "SELECT COUNT(*) FROM medicines WHERE stock < 20"
        ));
        stats.put("todayCompletionRate", todayAppointmentCount == 0
                ? 0
                : completedTodayCount * 100 / todayAppointmentCount);
        return stats;
    }

    public Map<String, Object> doctorStats(User user) {
        Map<String, Object> stats = new LinkedHashMap<>();
        Long doctorId = findDoctorId(user);
        stats.put("todayAppointments", countDoctorAppointmentsToday(doctorId));
        stats.put("pendingVisits", countDoctorAppointmentsByStatus(doctorId, "PENDING"));
        stats.put("completedVisits", countDoctorAppointmentsByStatus(doctorId, "COMPLETED"));
        stats.put("medicalRecordCount", countDoctorMedicalRecords(doctorId));
        stats.put("prescriptionCount", countDoctorPrescriptions(doctorId));
        stats.put("examinationCount", countDoctorExaminations(doctorId));
        return stats;
    }

    public Map<String, Object> patientStats(User user) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("appointmentCount", countMyAppointments(user));
        stats.put("medicalRecordCount", countMyMedicalRecords(user));
        stats.put("prescriptionCount", countMyPrescriptions(user));
        stats.put("examinationCount", countMyExaminations(user));
        stats.put("aiConsultationCount", countMyAiConsultations(user));
        return stats;
    }

    private int count(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count == null ? 0 : count;
    }

    private int countBySql(String sql) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count == null ? 0 : count;
    }

    private int countMyAppointments(User user) {
        if (user == null) {
            return 0;
        }
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM appointments a
                    JOIN patients p ON p.id = a.patient_id
                    WHERE p.user_id = ?
                    """, Integer.class, user.getId());
            return count == null ? 0 : count;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    private int countMyMedicalRecords(User user) {
        if (user == null) {
            return 0;
        }
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM medical_records mr
                    JOIN patients p ON p.id = mr.patient_id
                    WHERE p.user_id = ?
                    """, Integer.class, user.getId());
            return count == null ? 0 : count;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    private int countMyPrescriptions(User user) {
        if (user == null) {
            return 0;
        }
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM prescriptions p
                    JOIN patients patient ON patient.id = p.patient_id
                    WHERE patient.user_id = ?
                    """, Integer.class, user.getId());
            return count == null ? 0 : count;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    private int countMyExaminations(User user) {
        if (user == null) {
            return 0;
        }
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM examinations e
                    JOIN patients patient ON patient.id = e.patient_id
                    WHERE patient.user_id = ?
                    """, Integer.class, user.getId());
            return count == null ? 0 : count;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    private int countMyAiConsultations(User user) {
        if (user == null) {
            return 0;
        }
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM ai_consultations ai
                    JOIN patients patient ON patient.id = ai.patient_id
                    WHERE patient.user_id = ?
                    """, Integer.class, user.getId());
            return count == null ? 0 : count;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    private Long findDoctorId(User user) {
        if (user == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM doctors WHERE user_id = ?", Long.class, user.getId());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private int countDoctorAppointmentsToday(Long doctorId) {
        if (doctorId == null) {
            return 0;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM appointments
                WHERE doctor_id = ? AND appointment_date = CURRENT_DATE()
                """, Integer.class, doctorId);
        return count == null ? 0 : count;
    }

    private int countDoctorAppointmentsByStatus(Long doctorId, String status) {
        if (doctorId == null) {
            return 0;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM appointments
                WHERE doctor_id = ? AND status = ?
                """, Integer.class, doctorId, status);
        return count == null ? 0 : count;
    }

    private int countDoctorMedicalRecords(Long doctorId) {
        if (doctorId == null) {
            return 0;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM medical_records
                WHERE doctor_id = ?
                """, Integer.class, doctorId);
        return count == null ? 0 : count;
    }

    private int countDoctorPrescriptions(Long doctorId) {
        if (doctorId == null) {
            return 0;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM prescriptions
                WHERE doctor_id = ?
                """, Integer.class, doctorId);
        return count == null ? 0 : count;
    }

    private int countDoctorExaminations(Long doctorId) {
        if (doctorId == null) {
            return 0;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM examinations
                WHERE doctor_id = ?
                """, Integer.class, doctorId);
        return count == null ? 0 : count;
    }
}
