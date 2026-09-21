package com.county.tcm.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 配送任务:波次、配送员、改地址、签收与超时 */
@Entity
@Table(name = "delivery_tasks")
public class DeliveryTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Column(length = 20)
    private String wave;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private UserAccount courier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private DeliveryStatus status = DeliveryStatus.ASSIGNED;

    /** 签收时实际送达地址(可能为患者临时改后的地址) */
    @Column(length = 200)
    private String address;

    /** 老人代取:代取人 */
    @Column(length = 40)
    private String proxyName;

    private LocalDateTime assignedAt;
    private LocalDateTime outboundAt;
    private LocalDateTime signedAt;

    /** 承诺送达时间,超过即配送超时 */
    private LocalDateTime promisedAt;

    /** 签收人 */
    @Column(length = 40)
    private String signedBy;

    /** 超时分钟数(签收时回算) */
    private Integer timeoutMinutes;

    @Column(length = 300)
    private String remark;

    public DeliveryTask() {}

    public Long getId() { return id; }
    public Prescription getPrescription() { return prescription; }
    public void setPrescription(Prescription p) { this.prescription = p; }
    public String getWave() { return wave; }
    public void setWave(String s) { this.wave = s; }
    public UserAccount getCourier() { return courier; }
    public void setCourier(UserAccount u) { this.courier = u; }
    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus s) { this.status = s; }
    public String getAddress() { return address; }
    public void setAddress(String s) { this.address = s; }
    public String getProxyName() { return proxyName; }
    public void setProxyName(String s) { this.proxyName = s; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime t) { this.assignedAt = t; }
    public LocalDateTime getOutboundAt() { return outboundAt; }
    public void setOutboundAt(LocalDateTime t) { this.outboundAt = t; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDateTime t) { this.signedAt = t; }
    public LocalDateTime getPromisedAt() { return promisedAt; }
    public void setPromisedAt(LocalDateTime t) { this.promisedAt = t; }
    public String getSignedBy() { return signedBy; }
    public void setSignedBy(String s) { this.signedBy = s; }
    public Integer getTimeoutMinutes() { return timeoutMinutes; }
    public void setTimeoutMinutes(Integer i) { this.timeoutMinutes = i; }
    public String getRemark() { return remark; }
    public void setRemark(String s) { this.remark = s; }
}
