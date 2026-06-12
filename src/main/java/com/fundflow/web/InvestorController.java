package com.fundflow.web;

import com.fundflow.service.InvestorService;
import com.fundflow.web.dto.InvestorDtos.InvestorRequest;
import com.fundflow.web.dto.InvestorDtos.InvestorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/investors")
public class InvestorController {

    private final InvestorService investorService;

    public InvestorController(InvestorService investorService) {
        this.investorService = investorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvestorResponse create(@Valid @RequestBody InvestorRequest request) {
        return investorService.create(request);
    }

    @GetMapping
    public List<InvestorResponse> getAll() {
        return investorService.getAll();
    }

    @GetMapping("/{id}")
    public InvestorResponse get(@PathVariable Long id) {
        return investorService.get(id);
    }

    @PutMapping("/{id}")
    public InvestorResponse update(@PathVariable Long id, @Valid @RequestBody InvestorRequest request) {
        return investorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        investorService.delete(id);
    }
}
