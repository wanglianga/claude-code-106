package com.tcm.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 煎药操作记录(扫描药包、起止时间、异常气味、漏袋、复核人) */
@Getter
@Setter
@Entity
@Table(name = "decoct_records")
public class DecoctRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false)
    private DecoctTask task;

    @ManyToOne(fetch = FetchType.EAGER)
    private User operator;

    /** 扫描的药包码(须与处方编号一致) */
    @Column(length = 32)
    private String scanCode;

    private LocalDateTime scanTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 异常气味 */
    @Column(nullable = false)
    private Boolean abnormalSmell = false;

    @Column(length = 500)
    private String smellNote;

    /** 漏袋数 */
    @Column(nullable = false)
    private Integer leakedBags = 0;

    /** 复核人 */
    @Column(length = 64)
    private String reviewerName;

    @Column(length = 500)
    private String note;
}
