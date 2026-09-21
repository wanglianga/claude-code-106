package com.county.tcm.domain;

import jakarta.persistence.*;

/** 十八反 / 十九畏配伍禁忌规则 */
@Entity
@Table(name = "compatibility_rules")
public class CompatibilityRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String herbA;

    @Column(nullable = false, length = 40)
    private String herbB;

    /** 十八反 / 十九畏 */
    @Column(nullable = false, length = 10)
    private String ruleGroup;

    @Column(length = 120)
    private String description;

    public CompatibilityRule() {}
    public CompatibilityRule(String herbA, String herbB, String ruleGroup, String description) {
        this.herbA = herbA;
        this.herbB = herbB;
        this.ruleGroup = ruleGroup;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getHerbA() { return herbA; }
    public String getHerbB() { return herbB; }
    public String getRuleGroup() { return ruleGroup; }
    public String getDescription() { return description; }

    public boolean involves(String x, String y) {
        return (herbA.equals(x) && herbB.equals(y)) || (herbA.equals(y) && herbB.equals(x));
    }
}
