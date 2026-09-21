package com.county.tcm.repo;

import com.county.tcm.domain.DeliveryTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DeliveryTaskRepository extends JpaRepository<DeliveryTask, Long> {
    Optional<DeliveryTask> findByPrescriptionId(Long prescriptionId);
}
