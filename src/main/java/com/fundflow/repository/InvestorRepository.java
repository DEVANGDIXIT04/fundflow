package com.fundflow.repository;

import com.fundflow.domain.Investor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestorRepository extends JpaRepository<Investor, Long> {

    boolean existsByEmail(String email);
}
