package com.tcm.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 煎药任务(按处方剂数、锅号、浸泡时间、煎煮次数、包装袋数、配送波次安排) */
@Getter
@Setter
@Entity
@Table(name = "decoct_tasks")
public class DecoctTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, unique = true)
    private Prescription prescription;

    /** 锅号 */
    @Column(nullable = false, length = 16)
    private String potNo;

    /** 浸泡时间(分钟) */
    @Column(nullable = false)
    private Integer soakMinutes = 30;

    /** 煎煮次数 */
    @Column(nullable = false)
    private Integer decoctTimes = 2;

    /** 包装袋数 */
    @Column(nullable = false)
    private Integer bagCount;

    /** 配送波次 */
    @Column(nullable = false, length = 32)
    private String waveNo;

    /** 排队序号 */
    @Column(nullable = false)
    private Integer queuePosition;

    /** 夜间急煎优先 */
    @Column(nullable = false)
    private Boolean urgent = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DecoctStatus status = DecoctStatus.QUEUED;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
