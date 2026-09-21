package com.county.tcm.domain;

public enum IssueType {
    SHORTAGE("缺药"),
    ADDRESS_CHANGE("患者临时改地址"),
    SPECIAL_MISSED("特殊煎法遗漏"),
    BAG_DAMAGED("包装袋破损"),
    DELIVERY_TIMEOUT("配送超时"),
    DISCOMFORT("患者服药后不适"),
    INVOICE("诊所要求补开发票");

    private final String label;
    IssueType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
