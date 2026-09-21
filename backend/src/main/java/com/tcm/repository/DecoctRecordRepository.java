package com.tcm.repository;

import com.tcm.model.DecoctRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DecoctRecordRepository extends JpaRepository<DecoctRecord, Long> {
    List<DecoctRecord> findByTaskId(Long taskId);
    Optional<DecoctRecord> findFirstByTaskIdOrderByIdDesc(Long taskId);
}
