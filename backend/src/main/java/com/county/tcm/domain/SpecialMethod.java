package com.county.tcm.domain;

public enum SpecialMethod {
    NORMAL("常规"),
    FIRST_DECOCT("先煎"),
    LATER_ADD("后下"),
    WRAPPED("包煎"),
    DISSOLVED("烊化"),
    INFUSED("冲服");

    private final String label;
    SpecialMethod(String label) { this.label = label; }
    public String getLabel() { return label; }
}
