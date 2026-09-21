package com.county.tcm.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 代煎任务:锅号、浸泡、煎煮次数、包装袋数、配送波次、扫码与异常记录 */
@Entity
@Table(name = "decoct_tasks")
public class DecoctTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    /** 锅号,如 G-03 */
    @Column(length = 10)
    private String potNo;

    /** 浸泡时间(分钟) */
    private Integer soakMinutes;

    /** 煎煮次数 */
    private Integer boilTimes;

    /** 包装袋数 */
    private Integer bagCount;

    /** 配送波次,如 WAVE-AM-1 */
    @Column(length = 20)
    private String deliveryWave;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private DecoctStatus status = DecoctStatus.QUEUED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decoctor_id")
    private UserAccount decoctor;

    /** 扫码药包标识(抓药包条码) */
    @Column(length = 60)
    private String scannedBagCode;

    private LocalDateTime queuedAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    /** 异常气味记录 */
    @Column(length = 300)
    private String abnormalSmell;

    /** 漏袋数量 */
    private Integer missingBags = 0;

    /** 包装袋破损数量 */
    private Integer damagedBags = 0;

    /** 包装人 */
    @Column(length = 40)
    private String packer;

    /** 复核人 */
    @Column(length = 40)
    private String reviewer;

    @Column(length = 300)
    private String remark;

    public DecoctTask() {}

    public Long getId() { return id; }
    public Prescription getPrescription() { return prescription; }
    public void setPrescription(Prescription p) { this.prescription = p; }
    public String getPotNo() { return potNo; }
    public void setPotNo(String s) { this.potNo = s; }
    public Integer getSoakMinutes() { return soakMinutes; }
    public void setSoakMinutes(Integer i) { this.soakMinutes = i; }
    public Integer getBoilTimes() { return boilTimes; }
    public void setBoilTimes(Integer i) { this.boilTimes = i; }
    public Integer getBagCount() { return bagCount; }
    public void setBagCount(Integer i) { this.bagCount = i; }
    public String getDeliveryWave() { return deliveryWave; }
    public void setDeliveryWave(String s) { this.deliveryWave = s; }
    public DecoctStatus getStatus() { return status; }
    public void setStatus(DecoctStatus s) { this.status = s; }
    public UserAccount getDecoctor() { return decoctor; }
    public void setDecoctor(UserAccount u) { this.decoctor = u; }
    public String getScannedBagCode() { return scannedBagCode; }
    public void setScannedBagCode(String s) { this.scannedBagCode = s; }
    public LocalDateTime getQueuedAt() { return queuedAt; }
    public void setQueuedAt(LocalDateTime t) { this.queuedAt = t; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime t) { this.startedAt = t; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime t) { this.endedAt = t; }
    public String getAbnormalSmell() { return abnormalSmell; }
    public void setAbnormalSmell(String s) { this.abnormalSmell = s; }
    public Integer getMissingBags() { return missingBags; }
    public void setMissingBags(Integer i) { this.missingBags = i; }
    public Integer getDamagedBags() { return damagedBags; }
    public void setDamagedBags(Integer i) { this.damagedBags = i; }
    public String getPacker() { return packer; }
    public void setPacker(String s) { this.packer = s; }
    public String getReviewer() { return reviewer; }
    public void setReviewer(String s) { this.reviewer = s; }
    public String getRemark() { return remark; }
    public void setRemark(String s) { this.remark = s; }
}
