package com.county.tcm.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 履约事件流:一条处方上所有环节的动作留痕,形成完整履约链 */
@Entity
@Table(name = "fulfillment_events")
public class FulfillmentEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Stage stage;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(length = 500)
    private String detail;

    @Column(length = 40)
    private String operatorName;

    private LocalDateTime occurredAt;

    public FulfillmentEvent() {}
    public FulfillmentEvent(Prescription p, Stage stage, String action, String detail, String operatorName) {
        this.prescription = p;
        this.stage = stage;
        this.action = action;
        this.detail = detail;
        this.operatorName = operatorName;
        this.occurredAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Prescription getPrescription() { return prescription; }
    public Stage getStage() { return stage; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
    public String getOperatorName() { return operatorName; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
