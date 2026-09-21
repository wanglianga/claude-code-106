package com.county.tcm.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** 处方中的一味药(按剂剂量 + 特殊煎法) */
@Entity
@Table(name = "prescription_herbs")
public class PrescriptionHerb {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    /** 药材目录引用;缺药替代后仍可指向替代药材 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "herb_id")
    private Herb herb;

    /** 药味名称快照 */
    @Column(nullable = false, length = 40)
    private String herbName;

    /** 单剂剂量(克) */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal dosePerPacket;

    /** 特殊煎法:先煎/后下/包煎/烊化/冲服/常规 */
    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private SpecialMethod specialMethod = SpecialMethod.NORMAL;

    @Column(length = 120)
    private String note;

    /** 若为缺药替代,记录原药味名 */
    @Column(length = 40)
    private String substitutedFrom;

    public PrescriptionHerb() {}

    public Long getId() { return id; }
    public Prescription getPrescription() { return prescription; }
    public void setPrescription(Prescription p) { this.prescription = p; }
    public Herb getHerb() { return herb; }
    public void setHerb(Herb herb) { this.herb = herb; }
    public String getHerbName() { return herbName; }
    public void setHerbName(String herbName) { this.herbName = herbName; }
    public BigDecimal getDosePerPacket() { return dosePerPacket; }
    public void setDosePerPacket(BigDecimal d) { this.dosePerPacket = d; }
    public SpecialMethod getSpecialMethod() { return specialMethod; }
    public void setSpecialMethod(SpecialMethod m) { this.specialMethod = m; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getSubstitutedFrom() { return substitutedFrom; }
    public void setSubstitutedFrom(String s) { this.substitutedFrom = s; }

    /** 总用量 = 单剂剂量 × 剂数 */
    public BigDecimal totalQuantity(int doses) {
        return dosePerPacket.multiply(BigDecimal.valueOf(doses));
    }
}
