package com.county.tcm.domain;

public enum Role {
    PATIENT("患者"),
    CLINIC("诊所"),
    PHARMACIST("药师"),
    DECOCTOR("煎药员"),
    COURIER("配送员"),
    FINANCE("财务"),
    ADMIN("管理员");

    private final String label;
    Role(String label) { this.label = label; }
    public String getLabel() { return label; }
}
