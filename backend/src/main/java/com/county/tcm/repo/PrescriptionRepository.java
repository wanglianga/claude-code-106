package com.county.tcm.repo;

import com.county.tcm.domain.Prescription;
import com.county.tcm.domain.RxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    Optional<Prescription> findByRxNo(String rxNo);
    long countByStatus(RxStatus status);
    List<Prescription> findAllByOrderBySubmittedAtDesc();
    List<Prescription> findByStatusOrderBySubmittedAtAsc(RxStatus status);
}
