package com.tcm.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 药师审方记录 */
@Getter
@Setter
@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, unique = true)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.EAGER)
    private User pharmacist;

    /** 十八反十九畏核对结果 */
    @Column(length = 2000)
    private String conflictResult;

    /** 剂量异常核对结果 */
    @Column(length = 2000)
    private String doseResult;

    /** 缺药替代核对结果 */
    @Column(length = 2000)
    private String shortageResult;

    /** 医保目录核对结果 */
    @Column(length = 2000)
    private String insuranceResult;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReviewConclusion conclusion;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
