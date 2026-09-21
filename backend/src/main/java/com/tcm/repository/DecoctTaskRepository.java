package com.tcm.repository;

import com.tcm.model.DecoctStatus;
import com.tcm.model.DecoctTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DecoctTaskRepository extends JpaRepository<DecoctTask, Long> {
    Optional<DecoctTask> findByPrescriptionId(Long prescriptionId);
    List<DecoctTask> findByStatusOrderByUrgentDescCreatedAtAsc(DecoctStatus status);
    List<DecoctTask> findByStatusInOrderByUrgentDescCreatedAtAsc(List<DecoctStatus> statuses);
    long countByStatus(DecoctStatus status);
}
