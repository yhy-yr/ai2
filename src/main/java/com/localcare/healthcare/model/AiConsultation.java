package com.localcare.healthcare.model;

import java.time.LocalDateTime;

public class AiConsultation {

    private Long id;
    private Long patientId;
    private String symptoms;
    private String symptomSummary;
    private String suggestedDepartment;
    private String riskLevel;
    private String riskNotice;
    private String preVisitAdvice;
    private String disclaimer;
    private String source;
    private RiskAssessment risk;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getSymptomSummary() {
        return symptomSummary;
    }

    public void setSymptomSummary(String symptomSummary) {
        this.symptomSummary = symptomSummary;
    }

    public String getSuggestedDepartment() {
        return suggestedDepartment;
    }

    public void setSuggestedDepartment(String suggestedDepartment) {
        this.suggestedDepartment = suggestedDepartment;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRiskNotice() {
        return riskNotice;
    }

    public void setRiskNotice(String riskNotice) {
        this.riskNotice = riskNotice;
    }

    public String getPreVisitAdvice() {
        return preVisitAdvice;
    }

    public void setPreVisitAdvice(String preVisitAdvice) {
        this.preVisitAdvice = preVisitAdvice;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public RiskAssessment getRisk() {
        return risk;
    }

    public void setRisk(RiskAssessment risk) {
        this.risk = risk;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
