package com.fundflow.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public final class CommitmentDtos {

    private CommitmentDtos() {
    }

    public record CommitmentRequest(
            @NotNull Long investorId,
            @NotNull @Positive BigDecimal amount
    ) {
    }

    public record CommitmentUpdateRequest(
            @NotNull @Positive BigDecimal amount
    ) {
    }

    public record CommitmentResponse(
            Long id,
            Long fundId,
            Long investorId,
            String investorName,
            BigDecimal amount
    ) {
    }
}
