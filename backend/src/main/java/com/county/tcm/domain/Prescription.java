package com.county.tcm.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 处方:贯穿审方→抓药→代煎→配送→签收→回访的履约主记录 */
@Entity
@Table(name = "prescriptions")
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 处方号,展示用,如 RX20260921-0001 */
    @Column(nullable = false, unique = true, length = 30)
    private String rxNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_user_id")
    private UserAccount patientUser;

    /** 提交人:患者本人或诊所账号 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitter_id")
    private UserAccount submitter;

    /** 患者姓名(诊所代提交时手工填写) */
    @Column(nullable = false, length = 40)
    private String patientName;

    @Column(length = 20)
    private String patientPhone;

    @Column(length = 60)
    private String clinicName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @Column(length = 40)
    private String doctorName;

    /** 剂数 */
    @Column(nullable = false)
    private Integer doses;

    /** 特殊煎法整体说明(先煎后下等也在每味药上标注) */
    @Column(length = 500)
    private String specialDecoctionNote;

    /** 是否加糖 */
    private boolean addSugar = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PickupMethod pickupMethod;

    @Column(length = 200)
    private String address;

    @Column(length = 40)
    private String contactName;

    @Column(length = 20)
    private String contactPhone;

    /** 患者禁忌(过敏/妊娠/慢病等) */
    @Column(length = 500)
    private String contraindications;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private SettlementType settlementType;

    /** 夜间急煎 */
    private boolean nightUrgent = false;

    /** 诊所批量处方批次号 */
    @Column(length = 30)
    private String batchNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RxStatus status = RxStatus.PENDING_REVIEW;

    // ---- 费用(药师审方结算时确定) ----
    @Column(precision = 12, scale = 2)
    private BigDecimal herbAmount;
    @Column(precision = 12, scale = 2)
    private BigDecimal decoctFee;
    @Column(precision = 12, scale = 2)
    private BigDecimal deliveryFee;
    @Column(precision = 12, scale = 2)
    private BigDecimal nightSurcharge;
    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;
    @Column(precision = 12, scale = 2)
    private BigDecimal insurancePaid;
    @Column(precision = 12, scale = 2)
    private BigDecimal selfPaid;
    private boolean settled = false;

    private LocalDateTime submittedAt;
    private LocalDateTime finishedAt;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PrescriptionHerb> herbs = new ArrayList<>();

    public Prescription() {}

    public void addHerb(PrescriptionHerb h) {
        h.setPrescription(this);
        this.herbs.add(h);
    }

    public Long getId() { return id; }
    public String getRxNo() { return rxNo; }
    public void setRxNo(String rxNo) { this.rxNo = rxNo; }
    public UserAccount getPatientUser() { return patientUser; }
    public void setPatientUser(UserAccount u) { this.patientUser = u; }
    public UserAccount getSubmitter() { return submitter; }
    public void setSubmitter(UserAccount u) { this.submitter = u; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String s) { this.patientName = s; }
    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String s) { this.patientPhone = s; }
    public String getClinicName() { return clinicName; }
    public void setClinicName(String s) { this.clinicName = s; }
    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor d) { this.doctor = d; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String s) { this.doctorName = s; }
    public Integer getDoses() { return doses; }
    public void setDoses(Integer d) { this.doses = d; }
    public String getSpecialDecoctionNote() { return specialDecoctionNote; }
    public void setSpecialDecoctionNote(String s) { this.specialDecoctionNote = s; }
    public boolean isAddSugar() { return addSugar; }
    public void setAddSugar(boolean b) { this.addSugar = b; }
    public PickupMethod getPickupMethod() { return pickupMethod; }
    public void setPickupMethod(PickupMethod m) { this.pickupMethod = m; }
    public String getAddress() { return address; }
    public void setAddress(String s) { this.address = s; }
    public String getContactName() { return contactName; }
    public void setContactName(String s) { this.contactName = s; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String s) { this.contactPhone = s; }
    public String getContraindications() { return contraindications; }
    public void setContraindications(String s) { this.contraindications = s; }
    public SettlementType getSettlementType() { return settlementType; }
    public void setSettlementType(SettlementType t) { this.settlementType = t; }
    public boolean isNightUrgent() { return nightUrgent; }
    public void setNightUrgent(boolean b) { this.nightUrgent = b; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String s) { this.batchNo = s; }
    public RxStatus getStatus() { return status; }
    public void setStatus(RxStatus s) { this.status = s; }
    public BigDecimal getHerbAmount() { return herbAmount; }
    public void setHerbAmount(BigDecimal v) { this.herbAmount = v; }
    public BigDecimal getDecoctFee() { return decoctFee; }
    public void setDecoctFee(BigDecimal v) { this.decoctFee = v; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal v) { this.deliveryFee = v; }
    public BigDecimal getNightSurcharge() { return nightSurcharge; }
    public void setNightSurcharge(BigDecimal v) { this.nightSurcharge = v; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal v) { this.totalAmount = v; }
    public BigDecimal getInsurancePaid() { return insurancePaid; }
    public void setInsurancePaid(BigDecimal v) { this.insurancePaid = v; }
    public BigDecimal getSelfPaid() { return selfPaid; }
    public void setSelfPaid(BigDecimal v) { this.selfPaid = v; }
    public boolean isSettled() { return settled; }
    public void setSettled(boolean b) { this.settled = b; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime t) { this.submittedAt = t; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime t) { this.finishedAt = t; }
    public List<PrescriptionHerb> getHerbs() { return herbs; }
}
