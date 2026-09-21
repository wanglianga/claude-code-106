package com.county.tcm.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 用药回访:结果影响后续用药提醒、药房赔付与诊所合作评分 */
@Entity
@Table(name = "follow_ups")
public class FollowUp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id")
    private UserAccount operator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private FollowUpResult result;

    /** 不适症状描述 */
    @Column(length = 500)
    private String symptoms;

    /** 处置建议:停药就医/继续观察/药房赔付等 */
    @Column(length = 500)
    private String advice;

    /** 关联不适异常工单 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private Issue issue;

    /** 评分 1-5(患者对本次履约满意度) */
    private Integer satisfactionScore;

    private LocalDateTime followedAt;

    public FollowUp() {}

    public Long getId() { return id; }
    public Prescription getPrescription() { return prescription; }
    public void setPrescription(Prescription p) { this.prescription = p; }
    public UserAccount getOperator() { return operator; }
    public void setOperator(UserAccount u) { this.operator = u; }
    public FollowUpResult getResult() { return result; }
    public void setResult(FollowUpResult r) { this.result = r; }
    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String s) { this.symptoms = s; }
    public String getAdvice() { return advice; }
    public void setAdvice(String s) { this.advice = s; }
    public Issue getIssue() { return issue; }
    public void setIssue(Issue i) { this.issue = i; }
    public Integer getSatisfactionScore() { return satisfactionScore; }
    public void setSatisfactionScore(Integer i) { this.satisfactionScore = i; }
    public LocalDateTime getFollowedAt() { return followedAt; }
    public void setFollowedAt(LocalDateTime t) { this.followedAt = t; }
}
