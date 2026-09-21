package com.county.tcm.domain;

public enum DecoctStatus {
    QUEUED("已入队待浸泡"),
    DECOCTING("煎煮中"),
    PACKAGED("已包装待复核");

    private final String label;
    DecoctStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
