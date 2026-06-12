package com.fundflow.web.dto;

import com.fundflow.domain.CallStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class CapitalCallDtos {

    private CapitalCallDtos() {
    }

    public record CapitalCallRequest(
            @NotNull @Positive BigDecimal totalAmount,
            @NotNull @Future LocalDate dueDate
    ) {
    }

    public record CapitalCallResponse(
            Long id,
            Long fundId,
            int callNumber,
            BigDecimal totalAmount,
            LocalDate dueDate,
            CallStatus status,
            List<AllocationResponse> allocations
    ) {
    }

    public record AllocationResponse(
            Long id,
            Long investorId,
            String investorName,
            BigDecimal amount,
            boolean paid,
            Instant paidAt
    ) {
    }
}
