package com.county.tcm.repo;

import com.county.tcm.domain.FulfillmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FulfillmentEventRepository extends JpaRepository<FulfillmentEvent, Long> {
    List<FulfillmentEvent> findByPrescriptionIdOrderByOccurredAtAsc(Long prescriptionId);
}
