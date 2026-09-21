package com.tcm.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 处方履约异常事件(缺药/改地址/特殊煎法遗漏/包装破损/配送超时/服药不适/补开发票等) */
@Getter
@Setter
@Entity
@Table(name = "exception_events")
public class ExceptionEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false)
    private Prescription prescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExceptionType type;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExceptionStatus status = ExceptionStatus.OPEN;

    @ManyToOne(fetch = FetchType.EAGER)
    private User reportedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    private User handler;

    @Column(length = 1000)
    private String resolution;

    /** 药房赔付金额(元) */
    @Column(precision = 12, scale = 2)
    private BigDecimal compensation = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;
}
