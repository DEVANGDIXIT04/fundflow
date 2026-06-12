package com.fundflow.repository;

import com.fundflow.domain.Fund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundRepository extends JpaRepository<Fund, Long> {

    boolean existsByName(String name);
}
