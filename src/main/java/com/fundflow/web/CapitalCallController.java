package com.fundflow.web;

import com.fundflow.service.CapitalCallService;
import com.fundflow.web.dto.CapitalCallDtos.AllocationResponse;
import com.fundflow.web.dto.CapitalCallDtos.CapitalCallResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CapitalCallController {

    private final CapitalCallService capitalCallService;

    public CapitalCallController(CapitalCallService capitalCallService) {
        this.capitalCallService = capitalCallService;
    }

    @GetMapping("/capital-calls/{id}")
    public CapitalCallResponse get(@PathVariable Long id) {
        return capitalCallService.get(id);
    }

    @PostMapping("/capital-calls/{id}/issue")
    public CapitalCallResponse issue(@PathVariable Long id) {
        return capitalCallService.issue(id);
    }

    @PostMapping("/allocations/{id}/pay")
    public AllocationResponse pay(@PathVariable Long id) {
        return capitalCallService.pay(id);
    }
}
