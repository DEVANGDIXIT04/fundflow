package com.fundflow.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public final class FundDtos {

    private FundDtos() {
    }

    public record FundRequest(
            @NotBlank String name,
            @Min(1900) @Max(2100) int vintageYear,
            @NotNull @Positive BigDecimal targetSize,
            @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO currency code, e.g. USD") String currency
    ) {
    }

    public record FundResponse(
            Long id,
            String name,
            int vintageYear,
            BigDecimal targetSize,
            String currency
    ) {
    }

    public record FundSummaryResponse(
            Long fundId,
            String fundName,
            String currency,
            BigDecimal totalCommitted,
            BigDecimal totalCalled,
            BigDecimal totalPaid,
            BigDecimal outstanding
    ) {
    }
}
