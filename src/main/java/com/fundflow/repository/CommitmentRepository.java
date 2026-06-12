package com.fundflow.repository;

import com.fundflow.domain.Commitment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommitmentRepository extends JpaRepository<Commitment, Long> {

    List<Commitment> findByFundId(Long fundId);

    boolean existsByFundIdAndInvestorId(Long fundId, Long investorId);
}
