package com.county.tcm.repo;

import com.county.tcm.domain.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {
    List<FollowUp> findByPrescriptionIdOrderByFollowedAtAsc(Long prescriptionId);
}
