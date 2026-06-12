package com.fundflow.service;

import com.fundflow.domain.Investor;
import com.fundflow.exception.BusinessRuleException;
import com.fundflow.exception.ResourceNotFoundException;
import com.fundflow.repository.InvestorRepository;
import com.fundflow.web.dto.InvestorDtos.InvestorRequest;
import com.fundflow.web.dto.InvestorDtos.InvestorResponse;
import com.fundflow.web.dto.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InvestorService {

    private final InvestorRepository investorRepository;

    public InvestorService(InvestorRepository investorRepository) {
        this.investorRepository = investorRepository;
    }

    public InvestorResponse create(InvestorRequest request) {
        if (investorRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("An investor with email '" + request.email() + "' already exists");
        }
        return Mappers.toResponse(investorRepository.save(new Investor(request.name(), request.email())));
    }

    @Transactional(readOnly = true)
    public InvestorResponse get(Long id) {
        return Mappers.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<InvestorResponse> getAll() {
        return investorRepository.findAll().stream().map(Mappers::toResponse).toList();
    }

    public InvestorResponse update(Long id, InvestorRequest request) {
        Investor investor = find(id);
        investor.setName(request.name());
        investor.setEmail(request.email());
        return Mappers.toResponse(investor);
    }

    public void delete(Long id) {
        investorRepository.delete(find(id));
    }

    private Investor find(Long id) {
        return investorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investor", id));
    }
}
