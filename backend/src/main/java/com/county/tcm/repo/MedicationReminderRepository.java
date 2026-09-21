package com.county.tcm.repo;

import com.county.tcm.domain.MedicationReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MedicationReminderRepository extends JpaRepository<MedicationReminder, Long> {
    Optional<MedicationReminder> findByPrescriptionId(Long prescriptionId);
    List<MedicationReminder> findAllByOrderByNextRemindAtAsc();
}
