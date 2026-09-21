package com.county.tcm.repo;

import com.county.tcm.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findFirstByPrescriptionIdAndKindOrderByIssuedAtDesc(Long prescriptionId, String kind);
    List<Invoice> findAllByOrderByIssuedAtDesc();
}
