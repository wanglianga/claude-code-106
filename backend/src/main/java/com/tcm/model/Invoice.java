package com.tcm.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, unique = true)
    private Prescription prescription;

    @Column(nullable = false, unique = true, length = 48)
    private String invoiceNo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SettlementType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InvoiceStatus status = InvoiceStatus.ISSUED;

    @Column(length = 500)
    private String reissueReason;

    @Column(nullable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();

    private LocalDateTime reissuedAt;
}
