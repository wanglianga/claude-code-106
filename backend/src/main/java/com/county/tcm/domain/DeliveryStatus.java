package com.county.tcm.domain;

public enum DeliveryStatus {
    ASSIGNED("已接单待出库"),
    DELIVERING("配送中"),
    SIGNED("已签收");

    private final String label;
    DeliveryStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
