package com.fundflow.web;

import com.fundflow.service.CommitmentService;
import com.fundflow.web.dto.CommitmentDtos.CommitmentRequest;
import com.fundflow.web.dto.CommitmentDtos.CommitmentResponse;
import com.fundflow.web.dto.CommitmentDtos.CommitmentUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CommitmentController {

    private final CommitmentService commitmentService;

    public CommitmentController(CommitmentService commitmentService) {
        this.commitmentService = commitmentService;
    }

    @PostMapping("/funds/{fundId}/commitments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommitmentResponse create(@PathVariable Long fundId,
                                     @Valid @RequestBody CommitmentRequest request) {
        return commitmentService.create(fundId, request);
    }

    @GetMapping("/funds/{fundId}/commitments")
    public List<CommitmentResponse> getByFund(@PathVariable Long fundId) {
        return commitmentService.getByFund(fundId);
    }

    @PutMapping("/commitments/{id}")
    public CommitmentResponse update(@PathVariable Long id,
                                     @Valid @RequestBody CommitmentUpdateRequest request) {
        return commitmentService.updateAmount(id, request.amount());
    }

    @DeleteMapping("/commitments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        commitmentService.delete(id);
    }
}
