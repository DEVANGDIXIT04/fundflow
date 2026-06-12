package com.fundflow.service;

import com.fundflow.TestcontainersConfiguration;
import com.fundflow.domain.CallStatus;
import com.fundflow.web.dto.CapitalCallDtos.AllocationResponse;
import com.fundflow.web.dto.CapitalCallDtos.CapitalCallRequest;
import com.fundflow.web.dto.CapitalCallDtos.CapitalCallResponse;
import com.fundflow.web.dto.CommitmentDtos.CommitmentRequest;
import com.fundflow.web.dto.FundDtos.FundRequest;
import com.fundflow.web.dto.FundDtos.FundSummaryResponse;
import com.fundflow.web.dto.InvestorDtos.InvestorRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service-layer integration test against a real PostgreSQL (Testcontainers):
 * exercises the full capital call lifecycle including persistence, identity
 * generation, and the status rollup.
 */
@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(TestcontainersConfiguration.class)
class CapitalCallLifecycleIntegrationTest {

    @Autowired
    private FundService fundService;
    @Autowired
    private InvestorService investorService;
    @Autowired
    private CommitmentService commitmentService;
    @Autowired
    private CapitalCallService capitalCallService;

    @Test
    void fullLifecycle_createIssuePayAll_summaryReflectsEachStep() {
        // Arrange: a fund with two LPs committed 60/40
        Long fundId = fundService.create(new FundRequest(
                "Lifecycle Fund", 2026, new BigDecimal("10000000"), "USD")).id();
        Long lpA = investorService.create(new InvestorRequest("LP Alpha", "alpha@lp.example")).id();
        Long lpB = investorService.create(new InvestorRequest("LP Beta", "beta@lp.example")).id();
        commitmentService.create(fundId, new CommitmentRequest(lpA, new BigDecimal("6000000")));
        commitmentService.create(fundId, new CommitmentRequest(lpB, new BigDecimal("4000000")));

        // Create: DRAFT call allocated 60/40
        CapitalCallResponse call = capitalCallService.createWithAllocations(fundId,
                new CapitalCallRequest(new BigDecimal("1000000"), LocalDate.now().plusDays(30)));
        assertThat(call.status()).isEqualTo(CallStatus.DRAFT);
        assertThat(call.callNumber()).isEqualTo(1);
        assertThat(call.allocations())
                .extracting(AllocationResponse::amount)
                .satisfiesExactlyInAnyOrder(
                        amount -> assertThat(amount).isEqualByComparingTo("600000.00"),
                        amount -> assertThat(amount).isEqualByComparingTo("400000.00"));

        // A DRAFT call is not yet "called" capital
        FundSummaryResponse draftSummary = fundService.summary(fundId);
        assertThat(draftSummary.totalCalled()).isEqualByComparingTo("0");

        // Issue
        assertThat(capitalCallService.issue(call.id()).status()).isEqualTo(CallStatus.ISSUED);
        assertThat(fundService.summary(fundId).totalCalled()).isEqualByComparingTo("1000000");

        // Pay first allocation -> PARTIALLY_PAID
        AllocationResponse first = capitalCallService.pay(call.allocations().get(0).id());
        assertThat(first.paid()).isTrue();
        assertThat(capitalCallService.get(call.id()).status()).isEqualTo(CallStatus.PARTIALLY_PAID);

        FundSummaryResponse partialSummary = fundService.summary(fundId);
        assertThat(partialSummary.totalPaid()).isEqualByComparingTo(first.amount());
        assertThat(partialSummary.outstanding())
                .isEqualByComparingTo(new BigDecimal("1000000").subtract(first.amount()));

        // Pay second allocation -> PAID, nothing outstanding
        capitalCallService.pay(call.allocations().get(1).id());
        assertThat(capitalCallService.get(call.id()).status()).isEqualTo(CallStatus.PAID);

        FundSummaryResponse finalSummary = fundService.summary(fundId);
        assertThat(finalSummary.totalCommitted()).isEqualByComparingTo("10000000");
        assertThat(finalSummary.totalPaid()).isEqualByComparingTo("1000000");
        assertThat(finalSummary.outstanding()).isEqualByComparingTo("0");
    }

    @Test
    void callNumbersIncrementPerFund() {
        Long fundId = fundService.create(new FundRequest(
                "Numbering Fund", 2026, new BigDecimal("5000000"), "EUR")).id();
        Long lp = investorService.create(new InvestorRequest("LP Gamma", "gamma@lp.example")).id();
        commitmentService.create(fundId, new CommitmentRequest(lp, new BigDecimal("5000000")));

        CapitalCallRequest request = new CapitalCallRequest(
                new BigDecimal("100000"), LocalDate.now().plusDays(10));
        assertThat(capitalCallService.createWithAllocations(fundId, request).callNumber()).isEqualTo(1);
        assertThat(capitalCallService.createWithAllocations(fundId, request).callNumber()).isEqualTo(2);
        assertThat(capitalCallService.createWithAllocations(fundId, request).callNumber()).isEqualTo(3);
    }
}
