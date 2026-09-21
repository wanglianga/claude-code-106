package com.county.tcm.domain;

public enum FollowUpResult {
    GOOD("服药正常"),
    DISCOMFORT("服药后不适"),
    NO_ANSWER("未联系上"),
    REFUSED("拒绝回访");

    private final String label;
    FollowUpResult(String label) { this.label = label; }
    public String getLabel() { return label; }
}
