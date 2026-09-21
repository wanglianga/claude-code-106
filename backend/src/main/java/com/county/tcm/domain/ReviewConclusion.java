package com.county.tcm.domain;

public enum ReviewConclusion {
    PASS("审方通过"),
    PASS_WITH_SUBSTITUTE("缺药替代后通过"),
    REJECT("驳回处方");

    private final String label;
    ReviewConclusion(String label) { this.label = label; }
    public String getLabel() { return label; }
}
