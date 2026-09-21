package com.county.tcm.domain;

public enum IssueStatus {
    OPEN("待处理"),
    PROCESSING("处理中"),
    RESOLVED("已解决");

    private final String label;
    IssueStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
