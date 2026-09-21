package com.tcm.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "prescriptions")
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 处方编号，即煎药扫描码 */
    @Column(nullable = false, unique = true, length = 32)
    private String rxNo;

    @Column(nullable = false, length = 64)
    private String patientName;

    @Column(nullable = false, length = 32)
    private String patientPhone;

    private Integer patientAge;

    @Column(length = 8)
    private String patientGender;

    /** 患者禁忌(过敏史、妊娠等) */
    @Column(length = 1000)
    private String contraindications;

    @ManyToOne(fetch = FetchType.EAGER)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.EAGER)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.EAGER)
    private User submittedBy;

    /** 剂数 */
    @Column(nullable = false)
    private Integer doses;

    /** 特殊煎法总体说明(如先煎30分钟、文火等) */
    @Column(length = 1000)
    private String specialDecoction;

    /** 是否加糖 */
    @Column(nullable = false)
    private Boolean addSugar = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PickupMethod pickupMethod;

    @Column(length = 500)
    private String deliveryAddress;

    /** 老人代取人信息 */
    @Column(length = 64)
    private String proxyName;

    @Column(length = 32)
    private String proxyPhone;

    /** 夜间急煎 */
    @Column(nullable = false)
    private Boolean urgent = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PrescriptionStatus status = PrescriptionStatus.PENDING_REVIEW;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private SettlementType settlementType;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal insuranceAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal selfPayAmount;

    /** 诊所批量处方批次号 */
    @Column(length = 32)
    private String batchNo;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
