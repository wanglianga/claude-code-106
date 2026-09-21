package com.tcm.repository;

import com.tcm.model.ExceptionEvent;
import com.tcm.model.ExceptionStatus;
import com.tcm.model.ExceptionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExceptionEventRepository extends JpaRepository<ExceptionEvent, Long> {
    List<ExceptionEvent> findByPrescriptionIdOrderByCreatedAtDesc(Long prescriptionId);
    List<ExceptionEvent> findByStatusOrderByCreatedAtDesc(ExceptionStatus status);
    List<ExceptionEvent> findAllByOrderByCreatedAtDesc();
    long countByStatus(ExceptionStatus status);
    boolean existsByPrescriptionIdAndStatusNot(Long prescriptionId, ExceptionStatus status);
    boolean existsByPrescriptionIdAndTypeAndStatusNot(Long prescriptionId, ExceptionType type, ExceptionStatus status);
    List<ExceptionEvent> findByPrescriptionIdAndTypeAndStatusNot(Long prescriptionId, ExceptionType type, ExceptionStatus status);
}
