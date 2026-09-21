package com.tcm.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 十八反 / 十九畏 配伍禁忌 */
@Getter
@Setter
@Entity
@Table(name = "herb_conflicts")
public class HerbConflict {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false)
    private Herb herbA;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false)
    private Herb herbB;

    /** 十八反 / 十九畏 */
    @Column(nullable = false, length = 16)
    private String conflictType;

    @Column(length = 256)
    private String description;
}
