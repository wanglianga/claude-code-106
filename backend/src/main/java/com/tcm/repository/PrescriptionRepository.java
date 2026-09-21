package com.tcm.repository;

import com.tcm.model.Prescription;
import com.tcm.model.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    Optional<Prescription> findByRxNo(String rxNo);
    List<Prescription> findByStatusOrderByCreatedAtDesc(PrescriptionStatus status);
    List<Prescription> findAllByOrderByCreatedAtDesc();
    List<Prescription> findBySubmittedByIdOrderByCreatedAtDesc(Long userId);
    List<Prescription> findByClinicIdOrderByCreatedAtDesc(Long clinicId);
    List<Prescription> findByBatchNoOrderByCreatedAtAsc(String batchNo);
    long countByRxNoStartingWith(String prefix);
    long countByStatus(PrescriptionStatus status);
    long countByCreatedAtAfter(LocalDateTime time);
}
