package com.county.tcm.repo;

import com.county.tcm.domain.Herb;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HerbRepository extends JpaRepository<Herb, Long> {
    Optional<Herb> findByName(String name);
}
