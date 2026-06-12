package com.fundflow.service;

import com.fundflow.domain.CallStatus;
import com.fundflow.domain.CapitalCall;
import com.fundflow.domain.Commitment;
import com.fundflow.domain.Fund;
import com.fundflow.exception.BusinessRuleException;
import com.fundflow.exception.ResourceNotFoundException;
import com.fundflow.repository.CapitalCallRepository;
import com.fundflow.repository.CommitmentRepository;
import com.fundflow.repository.FundRepository;
import com.fundflow.web.dto.FundDtos.FundRequest;
import com.fundflow.web.dto.FundDtos.FundResponse;
import com.fundflow.web.dto.FundDtos.FundSummaryResponse;
import com.fundflow.web.dto.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class FundService {

    private final FundRepository fundRepository;
    private final CommitmentRepository commitmentRepository;
    private final CapitalCallRepository capitalCallRepository;

    public FundService(FundRepository fundRepository,
                       CommitmentRepository commitmentRepository,
                       CapitalCallRepository capitalCallRepository) {
        this.fundRepository = fundRepository;
        this.commitmentRepository = commitmentRepository;
        this.capitalCallRepository = capitalCallRepository;
    }

    public FundResponse create(FundRequest request) {
        if (fundRepository.existsByName(request.name())) {
            throw new BusinessRuleException("A fund named '" + request.name() + "' already exists");
        }
        Fund fund = new Fund(request.name(), request.vintageYear(), request.targetSize(), request.currency());
        return Mappers.toResponse(fundRepository.save(fund));
    }

    @Transactional(readOnly = true)
    public FundResponse get(Long id) {
        return Mappers.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<FundResponse> getAll() {
        return fundRepository.findAll().stream().map(Mappers::toResponse).toList();
    }

    public FundResponse update(Long id, FundRequest request) {
        Fund fund = find(id);
        fund.setName(request.name());
        fund.setVintageYear(request.vintageYear());
        fund.setTargetSize(request.targetSize());
        fund.setCurrency(request.currency());
        return Mappers.toResponse(fund);
    }

    public void delete(Long id) {
        Fund fund = find(id);
        if (!capitalCallRepository.findByFundIdOrderByCallNumber(id).isEmpty()) {
            throw new BusinessRuleException("Cannot delete a fund that has capital calls");
        }
        commitmentRepository.deleteAll(commitmentRepository.findByFundId(id));
        fundRepository.delete(fund);
    }

    /**
     * Aggregates the fund's lifecycle numbers. DRAFT calls are excluded:
     * capital is only considered "called" once the call has been issued.
     */
    @Transactional(readOnly = true)
    public FundSummaryResponse summary(Long fundId) {
        Fund fund = find(fundId);

        BigDecimal totalCommitted = commitmentRepository.findByFundId(fundId).stream()
                .map(Commitment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CapitalCall> issuedCalls = capitalCallRepository.findByFundIdOrderByCallNumber(fundId).stream()
                .filter(call -> call.getStatus() != CallStatus.DRAFT)
                .toList();

        BigDecimal totalCalled = issuedCalls.stream()
                .map(CapitalCall::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = issuedCalls.stream()
                .flatMap(call -> call.getAllocations().stream())
                .filter(allocation -> allocation.isPaid())
                .map(allocation -> allocation.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new FundSummaryResponse(
                fund.getId(),
                fund.getName(),
                fund.getCurrency(),
                totalCommitted,
                totalCalled,
                totalPaid,
                totalCalled.subtract(totalPaid)
        );
    }

    private Fund find(Long id) {
        return fundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fund", id));
    }
}
