package com.county.tcm.repo;

import com.county.tcm.domain.DecoctTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DecoctTaskRepository extends JpaRepository<DecoctTask, Long> {
    Optional<DecoctTask> findByPrescriptionId(Long prescriptionId);
}
