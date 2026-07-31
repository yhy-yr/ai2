package com.localcare.healthcare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.AiConsultation;
import com.localcare.healthcare.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiConsultationService {

    private static final String DISCLAIMER = "AI 结果仅供辅助参考，不能替代医生诊断。";
    private static final String DEEPSEEK_SYSTEM_PROMPT = """
            你是一个基层诊所智慧医疗系统中的 AI 辅助问诊模块。
            你的任务是根据患者症状，生成症状摘要、建议科室、风险等级、风险提示和就诊前建议。
            你不能替代医生诊断。
            你不能给出最终诊断。
            你不能推荐具体药品。
            你不能让患者自行用药。
            如果出现胸痛、呼吸困难、昏迷、剧烈头痛、持续高烧等高危症状，必须提示及时线下就医或拨打急救电话。
            请只返回 JSON，不要返回 Markdown，不要返回解释文字。

            返回 JSON 格式必须是：

            {
              "symptomSummary": "...",
              "suggestedDepartment": "...",
              "riskLevel": "LOW/MEDIUM/HIGH",
              "riskNotice": "...",
              "preVisitAdvice": "...",
              "disclaimer": "AI 结果仅供辅助参考，不能替代医生诊断。"
            }
            """;

    private final JdbcTemplate jdbcTemplate;
    private final DeepSeekService deepSeekService;
    private final RiskAssessmentService riskAssessmentService;

    private final RowMapper<AiConsultation> rowMapper = (rs, rowNum) -> {
        AiConsultation consultation = new AiConsultation();
        consultation.setId(rs.getLong("id"));
        consultation.setPatientId(rs.getLong("patient_id"));
        consultation.setSymptoms(rs.getString("symptoms"));
        consultation.setSymptomSummary(rs.getString("symptom_summary"));
        consultation.setSuggestedDepartment(rs.getString("suggested_department"));
        consultation.setRiskLevel(rs.getString("risk_level"));
        consultation.setRiskNotice(rs.getString("risk_notice"));
        consultation.setPreVisitAdvice(rs.getString("pre_visit_advice"));
        consultation.setDisclaimer(rs.getString("disclaimer"));
        consultation.setSource(readString(rs, "source"));
        consultation.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return consultation;
    };

    public AiConsultationService(
            JdbcTemplate jdbcTemplate,
            DeepSeekService deepSeekService,
            RiskAssessmentService riskAssessmentService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.deepSeekService = deepSeekService;
        this.riskAssessmentService = riskAssessmentService;
    }

    public AiConsultation analyze(User user, String symptoms) {
        Long patientId = requirePatientId(user);
        if (!StringUtils.hasText(symptoms)) {
            throw new BusinessException("symptoms 不能为空");
        }

        String normalizedSymptoms = symptoms.trim();
        AiConsultation result = analyzeByDeepSeek(normalizedSymptoms);
        if (result == null) {
            result = analyzeByLocalRules(normalizedSymptoms);
        }
        result.setPatientId(patientId);
        result.setCreatedAt(LocalDateTime.now());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        AiConsultation savedResult = result;
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
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
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, savedResult.getPatientId());
            ps.setString(2, savedResult.getSymptoms());
            ps.setString(3, savedResult.getSymptomSummary());
            ps.setString(4, savedResult.getSuggestedDepartment());
            ps.setString(5, savedResult.getRiskLevel());
            ps.setString(6, savedResult.getRiskNotice());
            ps.setString(7, savedResult.getPreVisitAdvice());
            ps.setString(8, savedResult.getDisclaimer());
            ps.setString(9, savedResult.getSource());
            ps.setObject(10, savedResult.getCreatedAt());
            return ps;
        }, keyHolder);

        return attachRisk(getById(keyHolder.getKey().longValue()));
    }

    public List<AiConsultation> my(User user) {
        Long patientId = requirePatientId(user);
        return jdbcTemplate.query("""
                SELECT * FROM ai_consultations
                WHERE patient_id = ?
                ORDER BY created_at DESC, id DESC
                """, rowMapper, patientId).stream().map(this::attachRisk).toList();
    }

    private AiConsultation analyzeByDeepSeek(String symptoms) {
        if (!deepSeekService.isAvailable()) {
            return null;
        }

        try {
            String userPrompt = "患者症状描述：\n" + symptoms + "\n\n请根据以上症状输出 JSON。";
            return deepSeekService.chat(DEEPSEEK_SYSTEM_PROMPT, userPrompt)
                    .flatMap(deepSeekService::parseJsonObject)
                    .map(json -> buildDeepSeekResult(symptoms, json))
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private AiConsultation buildDeepSeekResult(String symptoms, JsonNode json) {
        AiConsultation result = new AiConsultation();
        result.setSymptoms(symptoms);
        result.setSymptomSummary(requiredText(json, "symptomSummary"));
        result.setSuggestedDepartment(requiredText(json, "suggestedDepartment"));
        result.setRiskLevel(normalizeRiskLevel(requiredText(json, "riskLevel")));
        result.setRiskNotice(requiredText(json, "riskNotice"));
        result.setPreVisitAdvice(requiredText(json, "preVisitAdvice"));
        String disclaimer = text(json, "disclaimer");
        result.setDisclaimer(StringUtils.hasText(disclaimer) ? disclaimer : DISCLAIMER);
        result.setSource("DEEPSEEK");
        return result;
    }

    private AiConsultation analyzeByLocalRules(String symptoms) {
        AiConsultation result = new AiConsultation();
        result.setSymptoms(symptoms);
        result.setSymptomSummary("患者描述存在：" + symptoms + "。建议结合线下问诊、体格检查和必要检查进一步判断。");
        result.setDisclaimer(DISCLAIMER);
        result.setSource("LOCAL_RULE");

        if (containsAny(symptoms, "胸痛", "呼吸困难", "昏迷", "剧烈头痛", "持续高烧")) {
            result.setSuggestedDepartment("急诊 / 内科");
            result.setRiskLevel("HIGH");
            result.setRiskNotice("存在较高风险，建议及时线下就医或拨打急救电话。");
            result.setPreVisitAdvice("请尽快线下就医，途中避免自行驾车，保留症状发生时间和变化记录。");
            return result;
        }

        if (containsAny(symptoms, "小孩", "儿童", "宝宝")) {
            result.setSuggestedDepartment("儿科");
            result.setRiskLevel("MEDIUM");
            result.setRiskNotice("儿童症状变化可能较快，如精神差、持续发热或进食明显减少，请及时线下就医。");
            result.setPreVisitAdvice("就诊前记录体温、精神状态、饮食和排便情况，携带既往病历。");
            return result;
        }

        if (containsAny(symptoms, "发热", "发烧", "咳嗽", "咽痛", "嗓子疼")) {
            result.setSuggestedDepartment("呼吸内科");
            result.setRiskLevel("MEDIUM");
            result.setRiskNotice("如持续高热、呼吸困难或症状加重，请及时线下就医。");
            result.setPreVisitAdvice("就诊前注意休息，多饮水，记录体温变化。");
            return result;
        }

        if (containsAny(symptoms, "腹痛", "腹泻", "呕吐", "胃痛")) {
            result.setSuggestedDepartment("消化内科");
            result.setRiskLevel("MEDIUM");
            result.setRiskNotice("如出现剧烈腹痛、频繁呕吐、便血或明显脱水，请及时线下就医。");
            result.setPreVisitAdvice("就诊前记录腹痛部位、排便次数和饮食情况，避免刺激性食物。");
            return result;
        }

        if (containsAny(symptoms, "皮疹", "瘙痒", "红肿", "过敏")) {
            result.setSuggestedDepartment("皮肤科");
            result.setRiskLevel("LOW");
            result.setRiskNotice("如皮疹快速扩散、伴随呼吸不适或面部肿胀，请及时线下就医。");
            result.setPreVisitAdvice("就诊前避免抓挠，记录可能接触的食物、药物或过敏源。");
            return result;
        }

        result.setSuggestedDepartment("内科");
        result.setRiskLevel("LOW");
        result.setRiskNotice("当前描述未命中高风险规则，如症状持续或加重，请及时线下就医。");
        result.setPreVisitAdvice("就诊前整理症状持续时间、诱因和既往病史，便于医生判断。");
        return result;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private AiConsultation getById(Long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM ai_consultations WHERE id = ?", rowMapper, id);
    }

    private AiConsultation attachRisk(AiConsultation consultation) {
        if (consultation != null) {
            consultation.setRisk(riskAssessmentService.assess(consultation.getSymptoms()));
        }
        return consultation;
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

    private String normalizeRiskLevel(String riskLevel) {
        String value = riskLevel == null ? "" : riskLevel.trim().toUpperCase();
        if ("LOW".equals(value) || "MEDIUM".equals(value) || "HIGH".equals(value)) {
            return value;
        }
        return "MEDIUM";
    }

    private String readString(java.sql.ResultSet rs, String columnName) {
        try {
            return rs.getString(columnName);
        } catch (Exception e) {
            return null;
        }
    }

    private Long requirePatientId(User user) {
        if (user == null || !"PATIENT".equals(user.getRole())) {
            throw new BusinessException("只有患者可以使用 AI 问诊");
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
}
