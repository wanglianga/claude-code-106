package com.county.tcm.domain;

public enum RxStatus {
    PENDING_REVIEW("待审方"),
    REJECTED("已驳回"),
    APPROVED("审方通过待抓药"),
    DISPENSED("已抓药待煎煮"),
    READY_PICKUP("待自取"),
    DECOCTING("代煎中"),
    DECOCTED("煎煮完成待配送"),
    DELIVERING("配送中"),
    SIGNED("已签收"),
    FOLLOWED_UP("已回访");

    private final String label;
    RxStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
