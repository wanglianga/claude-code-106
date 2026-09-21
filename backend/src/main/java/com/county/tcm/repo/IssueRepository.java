package com.county.tcm.repo;

import com.county.tcm.domain.Issue;
import com.county.tcm.domain.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findAllByOrderByReportedAtDesc();
    List<Issue> findByPrescriptionIdOrderByReportedAtAsc(Long prescriptionId);
    long countByStatus(IssueStatus status);
}
