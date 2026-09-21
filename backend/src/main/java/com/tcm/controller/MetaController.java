package com.tcm.controller;

import com.tcm.model.Clinic;
import com.tcm.model.Doctor;
import com.tcm.model.Role;
import com.tcm.model.User;
import com.tcm.repository.ClinicRepository;
import com.tcm.repository.DoctorRepository;
import com.tcm.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final ClinicRepository clinics;
    private final DoctorRepository doctors;
    private final UserRepository users;

    public MetaController(ClinicRepository clinics, DoctorRepository doctors, UserRepository users) {
        this.clinics = clinics;
        this.doctors = doctors;
        this.users = users;
    }

    @GetMapping("/clinics")
    public List<Clinic> clinics() {
        return clinics.findAll();
    }

    @GetMapping("/doctors")
    public List<Doctor> doctors(@RequestParam(required = false) Long clinicId) {
        if (clinicId != null) {
            return doctors.findByClinicId(clinicId);
        }
        return doctors.findAll();
    }

    @GetMapping("/couriers")
    public List<User> couriers() {
        return users.findByRole(Role.COURIER);
    }
}
