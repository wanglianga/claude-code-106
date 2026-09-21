package com.tcm.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "herbs")
public class Herb {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(length = 64)
    private String category;

    /** 单价(元/克) */
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal unitPrice;

    /** 库存(克) */
    @Column(nullable = false)
    private Integer stockGram = 0;

    /** 常规最大日剂量(克)，超过则审方提示剂量异常 */
    @Column(nullable = false)
    private Integer maxDailyDoseGram = 30;

    /** 是否在医保目录内 */
    @Column(nullable = false)
    private Boolean inInsurance = true;
}
