package com.tcm.model;

public enum ExceptionType {
    SHORTAGE,         // 缺药
    ADDRESS_CHANGE,   // 患者临时改地址
    SPECIAL_MISSED,   // 特殊煎法遗漏
    BAG_DAMAGED,      // 包装袋破损/漏袋
    DELIVERY_TIMEOUT, // 配送超时
    DISCOMFORT,       // 患者服药后不适
    INVOICE_REISSUE,  // 诊所要求补开发票
    QUALITY_ABNORMAL  // 煎煮异常气味等质量问题
}
