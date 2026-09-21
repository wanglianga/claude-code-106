package com.county.tcm.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 抓药记录:扣减库存后的确认 */
@Entity
@Table(name = "dispense_records")
public class DispenseRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacist_id")
    private UserAccount pharmacist;

    /** 实际抓药总味数 */
    private Integer herbCount;

    @Column(length = 300)
    private String note;

    private LocalDateTime dispensedAt;

    public DispenseRecord() {}

    public Long getId() { return id; }
    public Prescription getPrescription() { return prescription; }
    public void setPrescription(Prescription p) { this.prescription = p; }
    public UserAccount getPharmacist() { return pharmacist; }
    public void setPharmacist(UserAccount u) { this.pharmacist = u; }
    public Integer getHerbCount() { return herbCount; }
    public void setHerbCount(Integer c) { this.herbCount = c; }
    public String getNote() { return note; }
    public void setNote(String s) { this.note = s; }
    public LocalDateTime getDispensedAt() { return dispensedAt; }
    public void setDispensedAt(LocalDateTime t) { this.dispensedAt = t; }
}
