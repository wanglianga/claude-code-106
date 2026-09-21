package com.county.tcm.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 药师审方记录:十八反十九畏、剂量异常、缺药替代、医保目录、结算结论 */
@Entity
@Table(name = "review_records")
public class ReviewRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacist_id")
    private UserAccount pharmacist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ReviewConclusion conclusion;

    /** 反畏 / 剂量 / 缺药 / 医保目录 等审核发现,逐条文本 */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "review_findings", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "finding", length = 300)
    private List<String> findings = new ArrayList<>();

    /** 缺药替代说明(原药→替代药) */
    @Column(length = 500)
    private String substitutions;

    /** 医保目录限制说明 */
    @Column(length = 500)
    private String insuranceNote;

    @Column(length = 500)
    private String remark;

    private LocalDateTime reviewedAt;

    public ReviewRecord() {}

    public Long getId() { return id; }
    public Prescription getPrescription() { return prescription; }
    public void setPrescription(Prescription p) { this.prescription = p; }
    public UserAccount getPharmacist() { return pharmacist; }
    public void setPharmacist(UserAccount u) { this.pharmacist = u; }
    public ReviewConclusion getConclusion() { return conclusion; }
    public void setConclusion(ReviewConclusion c) { this.conclusion = c; }
    public List<String> getFindings() { return findings; }
    public void setFindings(List<String> findings) { this.findings = findings; }
    public String getSubstitutions() { return substitutions; }
    public void setSubstitutions(String s) { this.substitutions = s; }
    public String getInsuranceNote() { return insuranceNote; }
    public void setInsuranceNote(String s) { this.insuranceNote = s; }
    public String getRemark() { return remark; }
    public void setRemark(String s) { this.remark = s; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime t) { this.reviewedAt = t; }
}
