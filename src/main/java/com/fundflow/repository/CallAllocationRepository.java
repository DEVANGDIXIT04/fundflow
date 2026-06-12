package com.fundflow.repository;

import com.fundflow.domain.CallAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallAllocationRepository extends JpaRepository<CallAllocation, Long> {
}
