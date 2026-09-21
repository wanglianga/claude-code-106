package com.county.tcm.repo;

import com.county.tcm.domain.ReviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {
    Optional<ReviewRecord> findFirstByPrescriptionIdOrderByReviewedAtDesc(Long prescriptionId);
}
