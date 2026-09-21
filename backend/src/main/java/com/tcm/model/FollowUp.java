package com.tcm.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 用药回访 */
@Getter
@Setter
@Entity
@Table(name = "follow_ups")
public class FollowUp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false)
    private Prescription prescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FollowUpResult result;

    @Column(length = 1000)
    private String discomfortDesc;

    /** 满意度 1-5，影响诊所合作评分 */
    private Integer satisfaction;

    /** 是否需要后续用药提醒 */
    @Column(nullable = false)
    private Boolean needReminder = false;

    @Column(length = 1000)
    private String note;

    @ManyToOne(fetch = FetchType.EAGER)
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
