package com.tcm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 处方药味明细 */
@Getter
@Setter
@Entity
@Table(name = "prescription_items")
public class PrescriptionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false)
    private Herb herb;

    /** 单剂剂量(克) */
    @Column(nullable = false)
    private Integer dosageG;

    /** 特殊煎法(先煎/后下/包煎/烊化/另煎/冲服) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SpecialHandling specialHandling = SpecialHandling.NONE;

    /** 缺药替代标记 */
    @Column(nullable = false)
    private Boolean substituted = false;

    @Column(length = 256)
    private String substituteNote;

    /** 下单时单价快照(元/克) */
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal unitPrice;
}
