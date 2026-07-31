package com.localcare.healthcare.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;

@Component
public class DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createUsersTable();
        createAuthSessionsTable();
        createDepartmentsTable();
        createMedicinesTable();
        createPatientsTable();
        createDoctorsTable();
        createAiConsultationsTable();
        ensureAiConsultationSourceColumn();
        createAppointmentsTable();
        createMedicalRecordsTable();
        createPrescriptionsTable();
        createPrescriptionItemsTable();
        createExaminationsTable();
        insertDefaultUsers();
        insertDefaultDepartments();
        insertDefaultMedicines();
        insertDefaultPatients();
        insertDefaultDoctors();
        if (!hasBusinessData()) {
            insertDefaultAiConsultations();
            insertDefaultAppointments();
            insertDefaultMedicalRecords();
            insertDefaultPrescriptions();
            insertDefaultExaminations();
        }
    }

    private boolean hasBusinessData() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM appointments", Integer.class);
        return count != null && count > 0;
    }

    private void createUsersTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password VARCHAR(100) NOT NULL,
                    role VARCHAR(20) NOT NULL,
                    real_name VARCHAR(50) NOT NULL,
                    phone VARCHAR(20),
                    status VARCHAR(20) NOT NULL,
                    created_at DATETIME NOT NULL
                )
                """);
    }

    private void createAuthSessionsTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS auth_sessions (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    session_token VARCHAR(100) NOT NULL UNIQUE,
                    expired_at DATETIME NOT NULL,
                    created_at DATETIME NOT NULL
                )
                """);
    }

    private void insertDefaultUsers() {
        ensureUser("admin", "123456", "ADMIN", "系统管理员", "13800000001");
        ensureUser("doctor", "123456", "DOCTOR", "张医生", "13800000002");
        ensureUser("patient", "123456", "PATIENT", "李明", "13800000003");
    }

    private void createDepartmentsTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS departments (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(50) NOT NULL,
                    description VARCHAR(300)
                )
                """);
    }

    private void createMedicinesTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS medicines (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL,
                    type VARCHAR(50),
                    specification VARCHAR(100),
                    price DECIMAL(10,2) NOT NULL DEFAULT 0,
                    stock INT NOT NULL DEFAULT 0,
                    usage_text VARCHAR(300)
                )
                """);
    }

    private void createPatientsTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS patients (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    gender VARCHAR(10),
                    age INT,
                    address VARCHAR(200),
                    allergy_history VARCHAR(500),
                    medical_history VARCHAR(500)
                )
                """);
    }

    private void createDoctorsTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS doctors (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    department_id BIGINT NOT NULL,
                    title VARCHAR(50),
                    specialty VARCHAR(200),
                    introduction VARCHAR(500)
                )
                """);
    }

    private void createAiConsultationsTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_consultations (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    patient_id BIGINT NOT NULL,
                    symptoms VARCHAR(1000) NOT NULL,
                    symptom_summary VARCHAR(1000),
                    suggested_department VARCHAR(100),
                    risk_level VARCHAR(20),
                    risk_notice VARCHAR(1000),
                    pre_visit_advice VARCHAR(1000),
                    disclaimer VARCHAR(300),
                    source VARCHAR(20) DEFAULT 'LOCAL_RULE',
                    created_at DATETIME NOT NULL
                )
                """);
    }

    private void ensureAiConsultationSourceColumn() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'ai_consultations'
                  AND column_name = 'source'
                """, Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE ai_consultations ADD COLUMN source VARCHAR(20) DEFAULT 'LOCAL_RULE'");
        }
    }

    private void createAppointmentsTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS appointments (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    patient_id BIGINT NOT NULL,
                    doctor_id BIGINT NOT NULL,
                    department_id BIGINT NOT NULL,
                    appointment_date DATE NOT NULL,
                    time_slot VARCHAR(50) NOT NULL,
                    symptom_description VARCHAR(1000),
                    status VARCHAR(20) NOT NULL,
                    created_at DATETIME NOT NULL
                )
                """);
    }

    private void createMedicalRecordsTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS medical_records (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    appointment_id BIGINT NOT NULL,
                    patient_id BIGINT NOT NULL,
                    doctor_id BIGINT NOT NULL,
                    chief_complaint VARCHAR(500),
                    present_illness VARCHAR(1000),
                    diagnosis VARCHAR(500),
                    treatment_plan VARCHAR(1000),
                    ai_draft VARCHAR(1000),
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME NOT NULL
                )
                """);
    }

    private void createPrescriptionsTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS prescriptions (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    medical_record_id BIGINT,
                    appointment_id BIGINT NOT NULL,
                    patient_id BIGINT NOT NULL,
                    doctor_id BIGINT NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    advice VARCHAR(1000),
                    created_at DATETIME NOT NULL
                )
                """);
    }

    private void createPrescriptionItemsTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS prescription_items (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    prescription_id BIGINT NOT NULL,
                    medicine_id BIGINT NOT NULL,
                    medicine_name VARCHAR(100) NOT NULL,
                    dosage VARCHAR(100),
                    frequency VARCHAR(100),
                    days INT,
                    quantity INT NOT NULL,
                    remark VARCHAR(300)
                )
                """);
    }

    private void createExaminationsTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS examinations (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    appointment_id BIGINT NOT NULL,
                    patient_id BIGINT NOT NULL,
                    doctor_id BIGINT NOT NULL,
                    exam_type VARCHAR(50),
                    exam_item VARCHAR(100),
                    result VARCHAR(1000),
                    conclusion VARCHAR(500),
                    created_at DATETIME NOT NULL
                )
                """);
    }

    private void insertDefaultDepartments() {
        ensureDepartment("内科", "常见内科疾病诊疗");
        ensureDepartment("外科", "外伤及普通外科诊疗");
        ensureDepartment("儿科", "儿童常见疾病诊疗");
        ensureDepartment("皮肤科", "皮肤过敏、皮疹等疾病诊疗");
        ensureDepartment("呼吸内科", "咳嗽、发热、呼吸系统疾病诊疗");
        ensureDepartment("消化内科", "腹痛、腹泻、胃肠疾病诊疗");
    }

    private void insertDefaultMedicines() {
        ensureMedicine("布洛芬", "退热止痛药", "0.2g*24片", "18.80", 120, "发热或疼痛时按说明服用");
        ensureMedicine("对乙酰氨基酚", "退热止痛药", "0.5g*12片", "12.50", 90, "用于普通感冒或流感引起的发热");
        ensureMedicine("阿莫西林", "抗生素", "0.25g*24粒", "26.00", 80, "遵医嘱使用，青霉素过敏者禁用");
        ensureMedicine("头孢克肟", "抗生素", "0.1g*6粒", "32.00", 60, "遵医嘱使用，注意过敏史");
        ensureMedicine("氯雷他定", "抗过敏药", "10mg*6片", "19.80", 75, "用于缓解过敏性鼻炎和皮肤瘙痒");
        ensureMedicine("蒙脱石散", "消化系统用药", "3g*10袋", "21.00", 45, "用于成人及儿童急慢性腹泻");
        ensureMedicine("奥美拉唑", "胃肠用药", "20mg*14粒", "24.60", 68, "用于胃酸相关症状，饭前服用");
        ensureMedicine("复方甘草片", "止咳化痰药", "100片", "15.00", 36, "用于镇咳祛痰，遵医嘱服用");
        ensureMedicine("生理盐水", "基础用药", "250ml", "6.50", 150, "用于冲洗或补液，按医嘱使用");
        ensureMedicine("维生素C片", "维生素类", "100mg*100片", "9.90", 25, "用于补充维生素C");
    }

    private void insertDefaultPatients() {
        ensurePatientProfile("patient", "男", 28, "北京市朝阳区", "无", "无");

        ensureUser("patient_wang", "123456", "PATIENT", "王芳", "13810000001");
        ensurePatientProfile("patient_wang", "女", 35, "上海市浦东新区", "青霉素过敏", "慢性胃炎");

        ensureUser("patient_zhao", "123456", "PATIENT", "赵强", "13810000002");
        ensurePatientProfile("patient_zhao", "男", 42, "广州市天河区", "无", "高血压");

        ensureUser("patient_chen", "123456", "PATIENT", "陈静", "13810000003");
        ensurePatientProfile("patient_chen", "女", 19, "杭州市西湖区", "花粉过敏", "无");

        ensureUser("patient_liu", "123456", "PATIENT", "刘洋", "13810000004");
        ensurePatientProfile("patient_liu", "男", 56, "成都市武侯区", "无", "2型糖尿病");
    }

    private void insertDefaultDoctors() {
        ensureDoctorProfile("doctor", "内科", "主治医师", "常见内科疾病、慢病管理", "负责基层常见病和慢性病诊疗。");

        ensureUser("doctor_surgery", "123456", "DOCTOR", "王外科", "13820000001");
        ensureDoctorProfile("doctor_surgery", "外科", "主治医师", "外伤处理、普通外科", "擅长基层外伤处理和普通外科常见问题。");

        ensureUser("doctor_child", "123456", "DOCTOR", "赵儿科", "13820000002");
        ensureDoctorProfile("doctor_child", "儿科", "副主任医师", "儿童发热、咳嗽、腹泻", "长期从事儿童常见疾病诊疗。");

        ensureUser("doctor_skin", "123456", "DOCTOR", "孙皮肤", "13820000003");
        ensureDoctorProfile("doctor_skin", "皮肤科", "主治医师", "皮疹、皮肤过敏、湿疹", "擅长皮肤过敏和常见皮肤病诊疗。");

        ensureUser("doctor_resp", "123456", "DOCTOR", "周呼吸", "13820000004");
        ensureDoctorProfile("doctor_resp", "呼吸内科", "主任医师", "咳嗽、发热、呼吸系统疾病", "擅长呼吸系统常见病诊疗。");

        ensureUser("doctor_digest", "123456", "DOCTOR", "吴消化", "13820000005");
        ensureDoctorProfile("doctor_digest", "消化内科", "主治医师", "腹痛、腹泻、胃肠疾病", "擅长胃肠道常见病诊疗。");
    }

    private void insertDefaultAiConsultations() {
        ensureAiConsultation(
                "patient",
                "发热、咳嗽、咽痛两天",
                "患者描述存在发热、咳嗽、咽痛等症状。",
                "呼吸内科",
                "MEDIUM",
                "如持续高热、呼吸困难或症状加重，请及时线下就医。",
                "就诊前注意休息，多饮水，记录体温变化。"
        );
        ensureAiConsultation(
                "patient_wang",
                "胃痛、腹泻一天",
                "患者描述存在胃痛、腹泻等消化道症状。",
                "消化内科",
                "MEDIUM",
                "如出现持续腹痛、呕血、黑便或脱水表现，请及时线下就医。",
                "就诊前清淡饮食，注意补液，记录腹泻次数。"
        );
    }

    private void insertDefaultAppointments() {
        ensureAppointment("patient", "doctor", "内科", 1, "15:00-16:00", "头晕、乏力一天", "PENDING");
        ensureAppointment("patient", "doctor_resp", "呼吸内科", 1, "09:00-10:00", "发热、咳嗽、咽痛两天", "PENDING");
        ensureAppointment("patient_wang", "doctor_digest", "消化内科", 2, "10:00-11:00", "胃痛、腹泻一天", "PENDING");
        ensureAppointment("patient_chen", "doctor_skin", "皮肤科", 3, "14:00-15:00", "皮疹、瘙痒反复出现", "PENDING");
    }

    private void insertDefaultMedicalRecords() {
        ensureMedicalRecord(
                "patient",
                "doctor",
                "头晕、乏力一天",
                "头晕、乏力一天。",
                "患者自述头晕、乏力一天，具体诊断需结合查体和必要检查判断。",
                "普通感冒待查",
                "建议注意休息，多饮水，观察体温和症状变化。",
                "该内容由本地规则根据预约症状自动生成，仅供医生书写病历时参考。"
        );
        ensureMedicalRecord(
                "patient_wang",
                "doctor_digest",
                "胃痛、腹泻一天",
                "胃痛、腹泻一天。",
                "患者自述胃痛、腹泻一天，具体诊断需结合查体和必要检查判断。",
                "急性胃肠炎待查",
                "建议清淡饮食，注意补液，必要时完善相关检查。",
                "该内容由本地规则根据预约症状自动生成，仅供医生书写病历时参考。"
        );
    }

    private void insertDefaultPrescriptions() {
        ensurePrescription(
                "patient",
                "doctor",
                "头晕、乏力一天",
                "注意休息，多饮水，按医嘱用药，症状加重及时复诊。",
                "维生素C片",
                "每次 1 片",
                "每日 2 次",
                3,
                1,
                "饭后服用"
        );
        ensurePrescription(
                "patient_wang",
                "doctor_digest",
                "胃痛、腹泻一天",
                "清淡饮食，注意补液，症状加重及时复诊。",
                "蒙脱石散",
                "每次 1 袋",
                "每日 3 次",
                3,
                1,
                "腹泻明显时服用"
        );
    }

    private void insertDefaultExaminations() {
        ensureExamination(
                "patient",
                "doctor",
                "头晕、乏力一天",
                "基础检查",
                "体温与血压",
                "体温正常，血压基本正常。",
                "暂未见明显异常，建议结合症状继续观察。"
        );
        ensureExamination(
                "patient_wang",
                "doctor_digest",
                "胃痛、腹泻一天",
                "实验室检查",
                "血常规",
                "白细胞轻度升高，其余指标基本正常。",
                "结合症状考虑胃肠道感染可能，建议按医嘱治疗。"
        );
    }

    private void ensureUser(String username, String password, String role, String realName, String phone) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, username);
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO users (username, password, role, real_name, phone, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                """, username, password, role, realName, phone, "ENABLED");
    }

    private void ensureDepartment(String name, String description) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM departments WHERE name = ?", Integer.class, name);
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update("INSERT INTO departments (name, description) VALUES (?, ?)", name, description);
    }

    private void ensureMedicine(String name, String type, String specification, String price, int stock, String usageText) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM medicines WHERE name = ?", Integer.class, name);
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO medicines (name, type, specification, price, stock, usage_text)
                VALUES (?, ?, ?, ?, ?, ?)
                """, name, type, specification, price, stock, usageText);
    }

    private void ensurePatientProfile(
            String username,
            String gender,
            int age,
            String address,
            String allergyHistory,
            String medicalHistory
    ) {
        Long userId = findUserId(username);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM patients WHERE user_id = ?", Integer.class, userId);
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO patients (user_id, gender, age, address, allergy_history, medical_history)
                VALUES (?, ?, ?, ?, ?, ?)
                """, userId, gender, age, address, allergyHistory, medicalHistory);
    }

    private void ensureDoctorProfile(
            String username,
            String departmentName,
            String title,
            String specialty,
            String introduction
    ) {
        Long userId = findUserId(username);
        Long departmentId = findDepartmentId(departmentName);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM doctors WHERE user_id = ?", Integer.class, userId);
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO doctors (user_id, department_id, title, specialty, introduction)
                VALUES (?, ?, ?, ?, ?)
                """, userId, departmentId, title, specialty, introduction);
    }

    private void ensureAppointment(
            String patientUsername,
            String doctorUsername,
            String departmentName,
            int daysFromToday,
            String timeSlot,
            String symptomDescription,
            String status
    ) {
        Long patientId = findPatientId(patientUsername);
        Long doctorId = findDoctorId(doctorUsername);
        Long departmentId = findDepartmentId(departmentName);
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM appointments
                WHERE patient_id = ?
                  AND doctor_id = ?
                  AND time_slot = ?
                  AND symptom_description = ?
                """, Integer.class, patientId, doctorId, timeSlot, symptomDescription);
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update("""
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
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                """,
                patientId,
                doctorId,
                departmentId,
                Date.valueOf(LocalDate.now().plusDays(daysFromToday)),
                timeSlot,
                symptomDescription,
                status);
    }

    private void ensureMedicalRecord(
            String patientUsername,
            String doctorUsername,
            String symptomDescription,
            String chiefComplaint,
            String presentIllness,
            String diagnosis,
            String treatmentPlan,
            String aiDraft
    ) {
        Long patientId = findPatientId(patientUsername);
        Long doctorId = findDoctorId(doctorUsername);
        Long appointmentId = jdbcTemplate.queryForObject("""
                SELECT id FROM appointments
                WHERE patient_id = ?
                  AND doctor_id = ?
                  AND symptom_description = ?
                ORDER BY id ASC
                LIMIT 1
                """, Long.class, patientId, doctorId, symptomDescription);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM medical_records WHERE appointment_id = ?",
                Integer.class,
                appointmentId
        );
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update("""
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
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                """,
                appointmentId,
                patientId,
                doctorId,
                chiefComplaint,
                presentIllness,
                diagnosis,
                treatmentPlan,
                aiDraft);
        jdbcTemplate.update("UPDATE appointments SET status = 'COMPLETED' WHERE id = ?", appointmentId);
    }

    private void ensureAiConsultation(
            String patientUsername,
            String symptoms,
            String symptomSummary,
            String suggestedDepartment,
            String riskLevel,
            String riskNotice,
            String preVisitAdvice
    ) {
        Long patientId = findPatientId(patientUsername);
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM ai_consultations
                WHERE patient_id = ? AND symptoms = ?
                """, Integer.class, patientId, symptoms);
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO ai_consultations (
                    patient_id,
                    symptoms,
                    symptom_summary,
                    suggested_department,
                    risk_level,
                    risk_notice,
                    pre_visit_advice,
                    disclaimer,
                    source,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """,
                patientId,
                symptoms,
                symptomSummary,
                suggestedDepartment,
                riskLevel,
                riskNotice,
                preVisitAdvice,
                "AI 结果仅供辅助参考，不能替代医生诊断。",
                "LOCAL_RULE");
    }

    private void ensurePrescription(
            String patientUsername,
            String doctorUsername,
            String symptomDescription,
            String advice,
            String medicineName,
            String dosage,
            String frequency,
            int days,
            int quantity,
            String remark
    ) {
        Long patientId = findPatientId(patientUsername);
        Long doctorId = findDoctorId(doctorUsername);
        Long appointmentId = findAppointmentId(patientId, doctorId, symptomDescription);
        Long medicalRecordId = findMedicalRecordId(appointmentId);
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM prescriptions
                WHERE appointment_id = ? AND doctor_id = ?
                """, Integer.class, appointmentId, doctorId);
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO prescriptions (
                    medical_record_id,
                    appointment_id,
                    patient_id,
                    doctor_id,
                    status,
                    advice,
                    created_at
                )
                VALUES (?, ?, ?, ?, 'ISSUED', ?, NOW())
                """, medicalRecordId, appointmentId, patientId, doctorId, advice);
        Long prescriptionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        Long medicineId = findMedicineId(medicineName);
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
                """, prescriptionId, medicineId, medicineName, dosage, frequency, days, quantity, remark);
        jdbcTemplate.update("UPDATE medicines SET stock = GREATEST(stock - ?, 0) WHERE id = ?", quantity, medicineId);
    }

    private void ensureExamination(
            String patientUsername,
            String doctorUsername,
            String symptomDescription,
            String examType,
            String examItem,
            String result,
            String conclusion
    ) {
        Long patientId = findPatientId(patientUsername);
        Long doctorId = findDoctorId(doctorUsername);
        Long appointmentId = findAppointmentId(patientId, doctorId, symptomDescription);
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM examinations
                WHERE appointment_id = ? AND exam_item = ?
                """, Integer.class, appointmentId, examItem);
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update("""
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
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                """, appointmentId, patientId, doctorId, examType, examItem, result, conclusion);
    }

    private Long findUserId(String username) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    private Long findDepartmentId(String departmentName) {
        return jdbcTemplate.queryForObject("SELECT id FROM departments WHERE name = ?", Long.class, departmentName);
    }

    private Long findPatientId(String username) {
        return jdbcTemplate.queryForObject("""
                SELECT p.id
                FROM patients p
                JOIN users u ON u.id = p.user_id
                WHERE u.username = ?
                """, Long.class, username);
    }

    private Long findDoctorId(String username) {
        return jdbcTemplate.queryForObject("""
                SELECT d.id
                FROM doctors d
                JOIN users u ON u.id = d.user_id
                WHERE u.username = ?
                """, Long.class, username);
    }

    private Long findMedicineId(String medicineName) {
        return jdbcTemplate.queryForObject("SELECT id FROM medicines WHERE name = ?", Long.class, medicineName);
    }

    private Long findAppointmentId(Long patientId, Long doctorId, String symptomDescription) {
        return jdbcTemplate.queryForObject("""
                SELECT id FROM appointments
                WHERE patient_id = ?
                  AND doctor_id = ?
                  AND symptom_description = ?
                ORDER BY id ASC
                LIMIT 1
                """, Long.class, patientId, doctorId, symptomDescription);
    }

    private Long findMedicalRecordId(Long appointmentId) {
        return jdbcTemplate.queryForObject("""
                SELECT id FROM medical_records
                WHERE appointment_id = ?
                ORDER BY id ASC
                LIMIT 1
                """, Long.class, appointmentId);
    }
}
