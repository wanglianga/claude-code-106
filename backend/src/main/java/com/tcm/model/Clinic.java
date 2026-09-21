package com.tcm.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "clinics")
public class Clinic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String name;

    @Column(length = 64)
    private String contactPerson;

    @Column(length = 32)
    private String phone;

    @Column(length = 256)
    private String address;

    /** 诊所合作评分(0-100)，回访结果会影响该评分 */
    @Column(nullable = false)
    private Double cooperationScore = 100.0;
}
