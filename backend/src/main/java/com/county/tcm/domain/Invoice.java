package com.county.tcm.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 发票(含诊所要求补开的发票) */
@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String invoiceNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Column(length = 80)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** NORMAL 正常开具 / REISSUED 补开 */
    @Column(nullable = false, length = 10)
    private String kind = "NORMAL";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finance_user_id")
    private UserAccount financeUser;

    private LocalDateTime issuedAt;

    public Invoice() {}

    public Long getId() { return id; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String s) { this.invoiceNo = s; }
    public Prescription getPrescription() { return prescription; }
    public void setPrescription(Prescription p) { this.prescription = p; }
    public String getTitle() { return title; }
    public void setTitle(String s) { this.title = s; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public String getKind() { return kind; }
    public void setKind(String s) { this.kind = s; }
    public Issue getIssue() { return issue; }
    public void setIssue(Issue i) { this.issue = i; }
    public UserAccount getFinanceUser() { return financeUser; }
    public void setFinanceUser(UserAccount u) { this.financeUser = u; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime t) { this.issuedAt = t; }
}
