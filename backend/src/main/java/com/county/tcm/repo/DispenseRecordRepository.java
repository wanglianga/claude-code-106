package com.county.tcm.repo;

import com.county.tcm.domain.DispenseRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispenseRecordRepository extends JpaRepository<DispenseRecord, Long> {
}
