package com.tcm.repository;

import com.tcm.model.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {
    List<FollowUp> findByPrescriptionIdOrderByCreatedAtDesc(Long prescriptionId);
}
