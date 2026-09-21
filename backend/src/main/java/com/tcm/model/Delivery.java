package com.tcm.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "deliveries")
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, unique = true)
    private Prescription prescription;

    /** 配送波次 */
    @Column(length = 32)
    private String waveNo;

    @ManyToOne(fetch = FetchType.EAGER)
    private User courier;

    @Column(length = 500)
    private String address;

    @Column(length = 64)
    private String receiverName;

    @Column(length = 32)
    private String receiverPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    private LocalDateTime dispatchedAt;

    private LocalDateTime signedAt;

    /** 签收人姓名 */
    @Column(length = 64)
    private String signedBy;

    /** 是否配送超时 */
    @Column(nullable = false)
    private Boolean timeout = false;

    /** 超时阈值(小时) */
    @Column(nullable = false)
    private Integer timeoutHours = 4;

    @Column(length = 500)
    private String note;
}
