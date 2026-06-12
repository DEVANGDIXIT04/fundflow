package com.fundflow.service;

import com.fundflow.domain.*;
import com.fundflow.exception.BusinessRuleException;
import com.fundflow.exception.ResourceNotFoundException;
import com.fundflow.repository.CallAllocationRepository;
import com.fundflow.repository.CapitalCallRepository;
import com.fundflow.repository.CommitmentRepository;
import com.fundflow.repository.FundRepository;
import com.fundflow.web.dto.CapitalCallDtos.AllocationResponse;
import com.fundflow.web.dto.CapitalCallDtos.CapitalCallRequest;
import com.fundflow.web.dto.CapitalCallDtos.CapitalCallResponse;
import com.fundflow.web.dto.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * Services return DTOs, never entities: mapping happens inside the transaction,
 * so lazy associations are still loadable (open-in-view is disabled).
 */
@Service
@Transactional
public class CapitalCallService {

    private final CapitalCallRepository capitalCallRepository;
    private final CallAllocationRepository allocationRepository;
    private final CommitmentRepository commitmentRepository;
    private final FundRepository fundRepository;

    public CapitalCallService(CapitalCallRepository capitalCallRepository,
                              CallAllocationRepository allocationRepository,
                              CommitmentRepository commitmentRepository,
                              FundRepository fundRepository) {
        this.capitalCallRepository = capitalCallRepository;
        this.allocationRepository = allocationRepository;
        this.commitmentRepository = commitmentRepository;
        this.fundRepository = fundRepository;
    }

    /**
     * Creates a capital call in DRAFT status and immediately splits it across
     * the fund's investors pro-rata by commitment. Allocations are visible for
     * review while the call is a draft; they only become payable after issue().
     */
    public CapitalCallResponse createWithAllocations(Long fundId, CapitalCallRequest request) {
        Fund fund = fundRepository.findById(fundId)
                .orElseThrow(() -> new ResourceNotFoundException("Fund", fundId));

        List<Commitment> commitments = commitmentRepository.findByFundId(fundId);
        if (commitments.isEmpty()) {
            throw new BusinessRuleException(
                    "Cannot create a capital call for fund '" + fund.getName() + "': it has no commitments");
        }

        int nextCallNumber = capitalCallRepository.findTopByFundIdOrderByCallNumberDesc(fundId)
                .map(call -> call.getCallNumber() + 1)
                .orElse(1);

        CapitalCall call = new CapitalCall(fund, nextCallNumber, request.totalAmount(), request.dueDate());
        allocateProRata(call, commitments);
        return Mappers.toResponse(capitalCallRepository.save(call));
    }

    /**
     * Pro-rata split: each investor owes totalAmount * (commitment / totalCommitted),
     * rounded DOWN to the cent. Rounding down leaves a remainder of at most a few
     * cents, which is assigned to the largest commitment so the allocations always
     * sum exactly to the call's total.
     */
    private void allocateProRata(CapitalCall call, List<Commitment> commitments) {
        BigDecimal totalCommitted = commitments.stream()
                .map(Commitment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Largest commitment last, so it absorbs the rounding remainder.
        List<Commitment> ordered = commitments.stream()
                .sorted(Comparator.comparing(Commitment::getAmount))
                .toList();

        BigDecimal allocatedSoFar = BigDecimal.ZERO;
        for (int i = 0; i < ordered.size(); i++) {
            Commitment commitment = ordered.get(i);
            BigDecimal amount;
            if (i == ordered.size() - 1) {
                amount = call.getTotalAmount().subtract(allocatedSoFar);
            } else {
                amount = call.getTotalAmount()
                        .multiply(commitment.getAmount())
                        .divide(totalCommitted, 2, RoundingMode.DOWN);
                allocatedSoFar = allocatedSoFar.add(amount);
            }
            call.addAllocation(new CallAllocation(call, commitment.getInvestor(), amount));
        }
    }

    @Transactional(readOnly = true)
    public CapitalCallResponse get(Long id) {
        return Mappers.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<CapitalCallResponse> getByFund(Long fundId) {
        if (!fundRepository.existsById(fundId)) {
            throw new ResourceNotFoundException("Fund", fundId);
        }
        return capitalCallRepository.findByFundIdOrderByCallNumber(fundId).stream()
                .map(Mappers::toResponse)
                .toList();
    }

    public CapitalCallResponse issue(Long callId) {
        CapitalCall call = find(callId);
        if (call.getStatus() != CallStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Capital call " + callId + " cannot be issued from status " + call.getStatus());
        }
        call.setStatus(CallStatus.ISSUED);
        return Mappers.toResponse(call);
    }

    /**
     * Marks one investor's allocation as paid and rolls the payment up to the
     * call's status: PAID when every allocation is settled, PARTIALLY_PAID otherwise.
     */
    public AllocationResponse pay(Long allocationId) {
        CallAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("CallAllocation", allocationId));

        CapitalCall call = allocation.getCapitalCall();
        if (call.getStatus() == CallStatus.DRAFT) {
            throw new BusinessRuleException("Cannot pay an allocation of a DRAFT capital call; issue it first");
        }
        if (allocation.isPaid()) {
            throw new BusinessRuleException("Allocation " + allocationId + " is already paid");
        }

        allocation.markPaid();

        boolean allPaid = call.getAllocations().stream().allMatch(CallAllocation::isPaid);
        call.setStatus(allPaid ? CallStatus.PAID : CallStatus.PARTIALLY_PAID);
        return Mappers.toResponse(allocation);
    }

    private CapitalCall find(Long id) {
        return capitalCallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CapitalCall", id));
    }
}
