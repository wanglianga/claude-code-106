package com.tcm.repository;

import com.tcm.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByPrescriptionId(Long prescriptionId);
}
