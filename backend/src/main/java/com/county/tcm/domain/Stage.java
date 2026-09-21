package com.county.tcm.domain;

/** 异常责任环节:把患者、诊所、药师、煎药房、配送、财务串到同一条履约记录中 */
public enum Stage {
    PATIENT("患者"),
    CLINIC("诊所"),
    PHARMACIST("药师"),
    DECOCTION("煎药房"),
    DELIVERY("配送"),
    FINANCE("财务");

    private final String label;
    Stage(String label) { this.label = label; }
    public String getLabel() { return label; }
}
