package com.tcm.model;

public enum PrescriptionStatus {
    PENDING_REVIEW,   // 待审方
    REVIEW_REJECTED,  // 审方驳回
    DISPENSING,       // 抓药中
    DECOCT_QUEUED,    // 代煎排队
    DECOCTING,        // 煎药中
    DECOCTED,         // 煎药完成(待配送)
    READY_PICKUP,     // 待取药(自提/代取)
    DELIVERING,       // 配送中
    SIGNED,           // 已签收
    FOLLOWED_UP,      // 已回访(有未结异常)
    COMPLETED,        // 已完成
    CANCELLED         // 已取消
}
