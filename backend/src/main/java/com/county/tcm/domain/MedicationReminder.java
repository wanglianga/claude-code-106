package com.county.tcm.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 后续用药提醒(回访结果影响是否生成/暂停) */
@Entity
@Table(name = "medication_reminders")
public class MedicationReminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Column(length = 60)
    private String patientName;

    @Column(length = 20)
    private String patientPhone;

    /** 每日提醒次数,如 2 = 早晚温服 */
    private Integer timesPerDay = 2;

    private LocalDateTime nextRemindAt;

    /** ACTIVE 正常 / PAUSED 不适暂停 / DONE 疗程结束 */
    @Column(length = 8)
    private String state = "ACTIVE";

    @Column(length = 300)
    private String note;

    private LocalDateTime createdAt;

    public MedicationReminder() {}

    public Long getId() { return id; }
    public Prescription getPrescription() { return prescription; }
    public void setPrescription(Prescription p) { this.prescription = p; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String s) { this.patientName = s; }
    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String s) { this.patientPhone = s; }
    public Integer getTimesPerDay() { return timesPerDay; }
    public void setTimesPerDay(Integer i) { this.timesPerDay = i; }
    public LocalDateTime getNextRemindAt() { return nextRemindAt; }
    public void setNextRemindAt(LocalDateTime t) { this.nextRemindAt = t; }
    public String getState() { return state; }
    public void setState(String s) { this.state = s; }
    public String getNote() { return note; }
    public void setNote(String s) { this.note = s; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
}
