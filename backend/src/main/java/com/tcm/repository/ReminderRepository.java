package com.tcm.repository;

import com.tcm.model.Reminder;
import com.tcm.model.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByPrescriptionIdOrderByRemindAtAsc(Long prescriptionId);
    List<Reminder> findByStatusAndRemindAtBefore(ReminderStatus status, LocalDateTime time);
    List<Reminder> findAllByOrderByRemindAtDesc();
}
