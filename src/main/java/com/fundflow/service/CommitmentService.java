package com.fundflow.service;

import com.fundflow.domain.Commitment;
import com.fundflow.domain.Fund;
import com.fundflow.domain.Investor;
import com.fundflow.exception.BusinessRuleException;
import com.fundflow.exception.ResourceNotFoundException;
import com.fundflow.repository.CommitmentRepository;
import com.fundflow.repository.FundRepository;
import com.fundflow.repository.InvestorRepository;
import com.fundflow.web.dto.CommitmentDtos.CommitmentRequest;
import com.fundflow.web.dto.CommitmentDtos.CommitmentResponse;
import com.fundflow.web.dto.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class CommitmentService {

    private final CommitmentRepository commitmentRepository;
    private final FundRepository fundRepository;
    private final InvestorRepository investorRepository;

    public CommitmentService(CommitmentRepository commitmentRepository,
                             FundRepository fundRepository,
                             InvestorRepository investorRepository) {
        this.commitmentRepository = commitmentRepository;
        this.fundRepository = fundRepository;
        this.investorRepository = investorRepository;
    }

    public CommitmentResponse create(Long fundId, CommitmentRequest request) {
        Fund fund = fundRepository.findById(fundId)
                .orElseThrow(() -> new ResourceNotFoundException("Fund", fundId));
        Investor investor = investorRepository.findById(request.investorId())
                .orElseThrow(() -> new ResourceNotFoundException("Investor", request.investorId()));
        if (commitmentRepository.existsByFundIdAndInvestorId(fundId, request.investorId())) {
            throw new BusinessRuleException(
                    "Investor " + investor.getName() + " already has a commitment to fund " + fund.getName());
        }
        return Mappers.toResponse(commitmentRepository.save(new Commitment(fund, investor, request.amount())));
    }

    @Transactional(readOnly = true)
    public List<CommitmentResponse> getByFund(Long fundId) {
        if (!fundRepository.existsById(fundId)) {
            throw new ResourceNotFoundException("Fund", fundId);
        }
        return commitmentRepository.findByFundId(fundId).stream().map(Mappers::toResponse).toList();
    }

    public CommitmentResponse updateAmount(Long commitmentId, BigDecimal amount) {
        Commitment commitment = find(commitmentId);
        commitment.setAmount(amount);
        return Mappers.toResponse(commitment);
    }

    public void delete(Long commitmentId) {
        commitmentRepository.delete(find(commitmentId));
    }

    private Commitment find(Long id) {
        return commitmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commitment", id));
    }
}
