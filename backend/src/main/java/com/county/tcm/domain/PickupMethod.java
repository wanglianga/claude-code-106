package com.county.tcm.domain;

public enum PickupMethod {
    DELIVERY("配送到家"),
    SELF_PICKUP("到店自取"),
    ELDER_PROXY("老人代取配送");

    private final String label;
    PickupMethod(String label) { this.label = label; }
    public String getLabel() { return label; }
}
