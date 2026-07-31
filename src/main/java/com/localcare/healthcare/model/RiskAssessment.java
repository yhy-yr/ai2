package com.localcare.healthcare.model;

import java.util.List;

public class RiskAssessment {

    private String level;
    private String levelText;
    private String color;
    private List<String> suggestedChecks;
    private String advice;

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLevelText() {
        return levelText;
    }

    public void setLevelText(String levelText) {
        this.levelText = levelText;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public List<String> getSuggestedChecks() {
        return suggestedChecks;
    }

    public void setSuggestedChecks(List<String> suggestedChecks) {
        this.suggestedChecks = suggestedChecks;
    }

    public String getAdvice() {
        return advice;
    }

    public void setAdvice(String advice) {
        this.advice = advice;
    }
}
