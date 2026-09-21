package com.county.tcm.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "doctors")
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(length = 30)
    private String title;

    /** 执业诊所名称 */
    @Column(length = 60)
    private String clinicName;

    @Column(length = 20)
    private String phone;

    /** 是否具备医保处方资质 */
    private boolean insuranceQualified = true;

    public Doctor() {}
    public Doctor(String name, String title, String clinicName, String phone, boolean insuranceQualified) {
        this.name = name;
        this.title = title;
        this.clinicName = clinicName;
        this.phone = phone;
        this.insuranceQualified = insuranceQualified;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getTitle() { return title; }
    public String getClinicName() { return clinicName; }
    public String getPhone() { return phone; }
    public boolean isInsuranceQualified() { return insuranceQualified; }
}
