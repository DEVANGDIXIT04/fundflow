package com.fundflow.web;

import com.fundflow.service.CapitalCallService;
import com.fundflow.service.FundService;
import com.fundflow.web.dto.CapitalCallDtos.CapitalCallRequest;
import com.fundflow.web.dto.CapitalCallDtos.CapitalCallResponse;
import com.fundflow.web.dto.FundDtos.FundRequest;
import com.fundflow.web.dto.FundDtos.FundResponse;
import com.fundflow.web.dto.FundDtos.FundSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/funds")
public class FundController {

    private final FundService fundService;
    private final CapitalCallService capitalCallService;

    public FundController(FundService fundService, CapitalCallService capitalCallService) {
        this.fundService = fundService;
        this.capitalCallService = capitalCallService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FundResponse create(@Valid @RequestBody FundRequest request) {
        return fundService.create(request);
    }

    @GetMapping
    public List<FundResponse> getAll() {
        return fundService.getAll();
    }

    @GetMapping("/{id}")
    public FundResponse get(@PathVariable Long id) {
        return fundService.get(id);
    }

    @PutMapping("/{id}")
    public FundResponse update(@PathVariable Long id, @Valid @RequestBody FundRequest request) {
        return fundService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        fundService.delete(id);
    }

    @GetMapping("/{id}/summary")
    public FundSummaryResponse summary(@PathVariable Long id) {
        return fundService.summary(id);
    }

    @PostMapping("/{id}/capital-calls")
    @ResponseStatus(HttpStatus.CREATED)
    public CapitalCallResponse createCapitalCall(@PathVariable Long id,
                                                 @Valid @RequestBody CapitalCallRequest request) {
        return capitalCallService.createWithAllocations(id, request);
    }

    @GetMapping("/{id}/capital-calls")
    public List<CapitalCallResponse> getCapitalCalls(@PathVariable Long id) {
        return capitalCallService.getByFund(id);
    }
}
