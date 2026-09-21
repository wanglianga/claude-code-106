package com.county.tcm.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 异常工单:缺药、临时改地址、特殊煎法遗漏、包装袋破损、配送超时、
 * 服药后不适、补开发票——把患者/诊所/药师/煎药房/配送/财务放在同一条处方履约记录中处理。
 */
@Entity
@Table(name = "issues")
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String issueNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private IssueStatus status = IssueStatus.OPEN;

    /** 责任/主办环节 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Stage ownerStage;

    @Column(length = 500)
    private String description;

    /** 定位信息:锅号 / 包装人 / 配送员等 */
    @Column(length = 200)
    private String locateInfo;

    @Column(length = 500)
    private String resolution;

    /** 药房赔付金额 */
    @Column(precision = 10, scale = 2)
    private BigDecimal compensation = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private UserAccount reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolver_id")
    private UserAccount resolver;

    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;

    public Issue() {}

    public Long getId() { return id; }
    public String getIssueNo() { return issueNo; }
    public void setIssueNo(String s) { this.issueNo = s; }
    public Prescription getPrescription() { return prescription; }
    public void setPrescription(Prescription p) { this.prescription = p; }
    public IssueType getType() { return type; }
    public void setType(IssueType t) { this.type = t; }
    public IssueStatus getStatus() { return status; }
    public void setStatus(IssueStatus s) { this.status = s; }
    public Stage getOwnerStage() { return ownerStage; }
    public void setOwnerStage(Stage s) { this.ownerStage = s; }
    public String getDescription() { return description; }
    public void setDescription(String s) { this.description = s; }
    public String getLocateInfo() { return locateInfo; }
    public void setLocateInfo(String s) { this.locateInfo = s; }
    public String getResolution() { return resolution; }
    public void setResolution(String s) { this.resolution = s; }
    public BigDecimal getCompensation() { return compensation; }
    public void setCompensation(BigDecimal v) { this.compensation = v; }
    public UserAccount getReporter() { return reporter; }
    public void setReporter(UserAccount u) { this.reporter = u; }
    public UserAccount getResolver() { return resolver; }
    public void setResolver(UserAccount u) { this.resolver = u; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime t) { this.reportedAt = t; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime t) { this.resolvedAt = t; }
}
