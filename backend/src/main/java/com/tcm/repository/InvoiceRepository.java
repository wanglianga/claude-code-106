package com.tcm.repository;

import com.tcm.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByPrescriptionId(Long prescriptionId);
}
