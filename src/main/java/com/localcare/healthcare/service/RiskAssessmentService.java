package com.localcare.healthcare.service;

import com.localcare.healthcare.model.RiskAssessment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskAssessmentService {

    public RiskAssessment assess(String symptoms) {
        String text = symptoms == null ? "" : symptoms.trim();
        if (containsAny(text, "胸痛", "呼吸困难", "昏迷", "大出血", "抽搐", "意识模糊")) {
            return build(
                    "HIGH",
                    "高风险",
                    "red",
                    List.of("血常规", "心电图", "胸部影像", "生命体征监测"),
                    "请尽快就医或急诊处理，由医生进一步评估。"
            );
        }

        if (containsAny(text, "发热", "胸闷", "剧烈头痛", "持续呕吐", "腹痛", "咳嗽")) {
            return build(
                    "MEDIUM",
                    "中风险",
                    "orange",
                    List.of("血常规", "C反应蛋白", "基础体格检查"),
                    "建议及时就诊并完善相关检查，由医生结合体征进一步判断。"
            );
        }

        return build(
                "LOW",
                "低风险",
                "green",
                List.of("基础体格检查"),
                "可先观察，若症状持续或加重请及时就医。"
        );
    }

    private RiskAssessment build(String level, String levelText, String color, List<String> suggestedChecks, String advice) {
        RiskAssessment assessment = new RiskAssessment();
        assessment.setLevel(level);
        assessment.setLevelText(levelText);
        assessment.setColor(color);
        assessment.setSuggestedChecks(suggestedChecks);
        assessment.setAdvice(advice);
        return assessment;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
