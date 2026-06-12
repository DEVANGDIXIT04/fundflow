package com.fundflow.web.dto;

import com.fundflow.domain.*;
import com.fundflow.web.dto.CapitalCallDtos.AllocationResponse;
import com.fundflow.web.dto.CapitalCallDtos.CapitalCallResponse;
import com.fundflow.web.dto.CommitmentDtos.CommitmentResponse;
import com.fundflow.web.dto.FundDtos.FundResponse;
import com.fundflow.web.dto.InvestorDtos.InvestorResponse;

public final class Mappers {

    private Mappers() {
    }

    public static FundResponse toResponse(Fund fund) {
        return new FundResponse(fund.getId(), fund.getName(), fund.getVintageYear(),
                fund.getTargetSize(), fund.getCurrency());
    }

    public static InvestorResponse toResponse(Investor investor) {
        return new InvestorResponse(investor.getId(), investor.getName(), investor.getEmail());
    }

    public static CommitmentResponse toResponse(Commitment commitment) {
        return new CommitmentResponse(commitment.getId(), commitment.getFund().getId(),
                commitment.getInvestor().getId(), commitment.getInvestor().getName(), commitment.getAmount());
    }

    public static CapitalCallResponse toResponse(CapitalCall call) {
        return new CapitalCallResponse(call.getId(), call.getFund().getId(), call.getCallNumber(),
                call.getTotalAmount(), call.getDueDate(), call.getStatus(),
                call.getAllocations().stream().map(Mappers::toResponse).toList());
    }

    public static AllocationResponse toResponse(CallAllocation allocation) {
        return new AllocationResponse(allocation.getId(), allocation.getInvestor().getId(),
                allocation.getInvestor().getName(), allocation.getAmount(),
                allocation.isPaid(), allocation.getPaidAt());
    }
}
