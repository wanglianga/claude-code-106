package com.tcm.repository;

import com.tcm.model.Delivery;
import com.tcm.model.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByPrescriptionId(Long prescriptionId);
    List<Delivery> findByStatusOrderByIdDesc(DeliveryStatus status);
    List<Delivery> findByStatus(DeliveryStatus status);
    List<Delivery> findByCourierIdOrderByIdDesc(Long courierId);
    long countByStatus(DeliveryStatus status);
}
