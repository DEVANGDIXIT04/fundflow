package com.fundflow.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class InvestorDtos {

    private InvestorDtos() {
    }

    public record InvestorRequest(
            @NotBlank String name,
            @NotBlank @Email String email
    ) {
    }

    public record InvestorResponse(
            Long id,
            String name,
            String email
    ) {
    }
}
