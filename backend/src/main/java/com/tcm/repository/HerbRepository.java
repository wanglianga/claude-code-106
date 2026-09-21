package com.tcm.repository;

import com.tcm.model.Herb;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HerbRepository extends JpaRepository<Herb, Long> {
    Optional<Herb> findByName(String name);
    List<Herb> findAllByOrderByNameAsc();
}
