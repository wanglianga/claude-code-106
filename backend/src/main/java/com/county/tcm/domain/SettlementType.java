package com.county.tcm.domain;

public enum SettlementType {
    INSURANCE("医保结算"),
    SELF_PAY("自费");

    private final String label;
    SettlementType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
