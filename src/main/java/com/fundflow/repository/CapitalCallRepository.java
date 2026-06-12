package com.fundflow.repository;

import com.fundflow.domain.CapitalCall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CapitalCallRepository extends JpaRepository<CapitalCall, Long> {

    List<CapitalCall> findByFundIdOrderByCallNumber(Long fundId);

    Optional<CapitalCall> findTopByFundIdOrderByCallNumberDesc(Long fundId);
}
