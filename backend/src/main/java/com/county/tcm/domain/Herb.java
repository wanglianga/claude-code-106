package com.county.tcm.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** 药材目录与库存 */
@Entity
@Table(name = "herbs")
public class Herb {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String name;

    @Column(length = 60)
    private String pinyin;

    @Column(length = 10)
    private String unit = "g";

    /** 每克单价 */
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /** 库存克数 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal stock = BigDecimal.ZERO;

    /** 是否在医保目录内 */
    private boolean insuranceCovered = true;

    /** 单剂常用安全上限(克),超出触发剂量异常 */
    @Column(precision = 8, scale = 2)
    private BigDecimal maxDosePerPacket;

    private boolean active = true;

    public Herb() {}
    public Herb(String name, String pinyin, BigDecimal unitPrice, BigDecimal stock,
                boolean insuranceCovered, BigDecimal maxDosePerPacket) {
        this.name = name;
        this.pinyin = pinyin;
        this.unitPrice = unitPrice;
        this.stock = stock;
        this.insuranceCovered = insuranceCovered;
        this.maxDosePerPacket = maxDosePerPacket;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPinyin() { return pinyin; }
    public String getUnit() { return unit; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getStock() { return stock; }
    public boolean isInsuranceCovered() { return insuranceCovered; }
    public BigDecimal getMaxDosePerPacket() { return maxDosePerPacket; }
    public boolean isActive() { return active; }

    public void setStock(BigDecimal stock) { this.stock = stock; }
}
